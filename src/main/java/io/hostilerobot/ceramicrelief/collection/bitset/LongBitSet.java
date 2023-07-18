package io.hostilerobot.ceramicrelief.collection.bitset;

// only a 64 bit range that it can set. however
public class LongBitSet implements IBitSet{
    private int offset = 0; // represents the lowest set bit
    // todo - make highest into a byte length to represent the size highest - offset. Will reduce space by 24 bits
    private int highest = 0; // represents the highest set bit
    private long mask = 0; // current mask
    public boolean get(int index) {
        int pos = index - offset;
        return (pos & Integer.MIN_VALUE) != 0 && pos <= Long.SIZE &&
                (mask & (index - offset)) != 0;
    }
    public boolean canSet(int index) {
        return 64 + offset >= index && highest - 64 <= index && index >= 0;
    }

    public boolean isEmpty() {
        return mask == 0L;
    }
    public boolean canOr(IBitSet other) {
        if(this.isEmpty() || other.isEmpty())
            return true;
        if(!(other.getClass() == LongBitSet.class) && ((other.getMaxIndex() - other.getMinIndex()) & ~63) != 0) {
            return false;
        }
        if(this.offset > other.getMinIndex()) {
            return highest - other.getMinIndex() <= 64;
        } else {
            return other.getMinIndex() - offset <= 64;
        }
    }
    public void set(int index) {
        if(!canSet(index))
            throw new IllegalStateException("index " + index + " is out of bitset's capacity");
        if(index < offset) {
                    // shift the mask over so index is now the lowest bit
            mask = (mask << (offset - index) | 1);
            offset = index; // index we set is the new offset
            return;
        } else if (index > highest){
            highest = index;
        }
        mask = mask | (1L << (index - offset));
    }
    public void or(IBitSet other) {
        if(!canOr(other))
            throw new IllegalStateException("bitset " + other + " is out of this capacity");
        // we may need to slide the current offset
        int thisOffset = offset;
        int otherOffset = other.getMinIndex();
        int neg = (thisOffset - otherOffset) >> 31; // 0xFFFFFFFF if offset < other.offset, 0 otherwise
        int minOffset = (thisOffset & neg) | (otherOffset & ~neg); // min value for offset

        // new mask composes the words at the minimum offset
        mask = wordAt(minOffset) | other.wordAt(minOffset);
        offset = minOffset;
        int thisHighest = highest;
        int otherHighest = other.getMaxIndex();
        int neg2 = (thisHighest - otherHighest) >> 31;
        highest = (thisHighest & ~neg2) | (otherHighest & neg2);
    }
    public void and(IBitSet other) {
        // bounds can only get smaller with AND operation, so we can just get the relevant bits from or and AND them together
        mask = other.wordAt(offset) & mask;
        rejigBounds();
    }
    private void rejigMin() {
        // if 64 zeroes then we want trailing to be zero (no effect)
        int trailing = (Long.numberOfTrailingZeros(mask) & 0x3F);
        mask = mask >>> trailing;
        offset = offset - trailing;
    }
    private void rejigMax() {
        int firstOne = 64 - Long.numberOfLeadingZeros(mask);
        // min is already set to 1. no bitshifting required, but set the higher bound
        highest = offset + firstOne;
    }
    private void rejigBounds() {
        rejigMin();
        rejigMax();
    }

    public String toString() {
        return Long.toBinaryString(mask) + ":" + offset;
    }
    @Override
    public int getMinIndex() {
        return offset;
    }
    @Override
    public int getMaxIndex() {
        return highest;
    }

    @Override
    public long wordAt(int index) {
        int thisOffset = offset;
        long thisMask = mask;
        int sub = thisOffset - index;
        int parity = sub >> 31; // 0xFFFF FFFF if thisOffset < index, 0 otherwise
        int shr = sub & ~parity;
        int neg = ~sub + 1; // negate
        int shl = neg & parity;

        // caveat - when shift is greater than 63 then it will wrap as if it were modded by 64.
        //          we don't want this behavior.

        // note that any words outside of a 63 bit shift will just be zero.
        // thus we can just find if the shift would be greater than 63, and just return zero as a final result

        // triple xor hack
        long shift = thisMask ^ (thisMask << shl) ^ (thisMask >>> shr);
        /*long shift = ((thisMask << neg) & parity) // alternative if existing doesn't work
                | ((thisMask >>> sub) & ~parity); */

        // shift is our word. however we zero it out if |thisOffset - index| >= 64

        long clamp = shift | (63L - shr - shl) >> 63;
        return shift & clamp;
    }
}

package io.hostilerobot.ceramicrelief.collection.bitset;


/**
 * idea: we have a smaller bitset we can utilize by keeping a "viability window" that can slide around
 * the current maximum capacity is words.length * Long.SIZE
 * this also permits us to create a mapping from any valid integer to boolean, not just positive integers
 */
public class OffBitSet implements IBitSet {
    // always equal to the minimum index
    private final static int POW2_BITS_PER_WORD = 6;
    private final static int BITS_PER_WORD = 1 << POW2_BITS_PER_WORD;
    private final static int NEGATIVE_MASK = Integer.MIN_VALUE;

    // offset to the index of the first word. Note that the min index may be different, as on occasions we may just
    // want to insert a new index in the array and copy the rest over via System.arrayCopy
    // or just generally make modifications without having to refactor
    private int offset = 0;
    private int currentMinIdx = 0;
    private int currentMaxIdx = 0;
    // invariant: offset <= currentMinIdx <= currentMaxIdx

    private long[] words;


    public OffBitSet() {
        this(BITS_PER_WORD * 2);
    }
    public OffBitSet(int currentMaxIdx) {
        initWords(currentMaxIdx);
    }

    private void initWords(int nbits) {
        words = new long[wordIndex(nbits-1) + 1];
    }

    @Override
    public boolean isEmpty() {
        // special case: words.length == 0
        return currentMinIdx == currentMaxIdx && words.length != 0 && words[wordIndex(currentMinIdx)] == 0;
    }

    @Override
    public boolean canOr(IBitSet other) {
        // always true - infinite capacity
        return true;
    }

    @Override
    public boolean canSet(int index) {
        // always true - infinite capacity
        return true;
    }

    public boolean get(int bitIdx) {
        int word;
        return bitIdx <= currentMaxIdx
                && ((word = wordIndex(bitIdx)) & NEGATIVE_MASK) == 0 // don't want to look up a negative index
                && (words[word] & (1L << (bitIdx - offset))) != 0; // since shifting in java is masked by 0x3F, it can go over 63 and we will still get the right position
    }

    @Override
    public void set(int index) {

    }

    @Override
    public void or(IBitSet other) {
        int thisMin = currentMinIdx;
        int otherMin = other.getMinIndex();

        int thisMax = currentMaxIdx;
        int otherMax = other.getMaxIndex();

        // note - new min will be min(thisMin, otherMin), new max will be max(thisMax, otherMax)
        int maskMin = (thisMin - otherMin) >> 31; // -1 if thisMin < otherMin, 0 otherwise.
        int newMin = (thisMin & maskMin) | (otherMin & ~maskMin);

        int maskMax = (thisMax - otherMax) >> 31;
        int newMax = (thisMax & ~maskMax) | (otherMax & maskMax);


        // simple, stupid implementation: run from max(thisMin, otherMax) to min(thisMax, otherMax)
        // and OR each word from other into this one
        int startIdx = newMin ^ thisMin ^ otherMin; // one of these will cancel out, and will return the max of the two mins
        int endIdx = newMax ^ thisMax ^ otherMax;   // same logic as above

        int startWordIdx = wordIndex(startIdx);
        // todo - verify bounds
        // todo -   * what if index starts at the beginning of a word? What if we increment such that startIdx + 64 == endIdx
        while(startIdx < endIdx) {
            words[startWordIdx++] |= other.wordAt(startIdx);
            startIdx += 64;
        }

        currentMinIdx = newMin;
        currentMaxIdx = newMax;
        // todo - we may need to resize words to fit the entire space.
    }

    @Override
    public void and(IBitSet other) {
        if(other.getClass() == OffBitSet.class) {
            // optimize so we don't just call wordAt a bunch of times.
            OffBitSet otherBs = (OffBitSet) other;


        } else {

        }
    }

    @Override
    public int getMinIndex() {
        return currentMinIdx;
    }

    @Override
    public int getMaxIndex() {
        return currentMaxIdx;
    }


    // retrieve a word at a given index, but we do NOT check the bounds
    private long wordAt_unsafe(int index) {
        int wordIdx1 = wordIndex(index);
        int shift = (index - offset);
        int mod64 = shift & 0b111111;
        int wordIdx2 = wordIdx1 + 1 +
                ((mod64 | (~mod64 + 1)) >> 31); // use the same index if (index - offset) % 64 == 0
                                                // mask is -1 if so, and 0 otherwise

        // when shift % 64 == 0, then we will have words[wordIdx1] | words[wordIdx2] == words[wordIdx1]
        return words[wordIdx1] << (64 - shift) | words[wordIdx2] >>> shift;
    }
    @Override
    public long wordAt(int index) {
        // word starts at index and goes till index + 63
        int wordIdx1 = wordIndex(index);
        int wordIdx2 = wordIdx1 + 1; // the next word

        // shift word1 to the left, word2 to the right
        int shift = (index - offset);
        int mod64 = shift & 0b111111;

        int boundsSwitchMask =
            // first index out of bounds and should be zero
              (wordIdx1 & NEGATIVE_MASK) >> 31  // first index negative
            | ((words.length - wordIdx2) & NEGATIVE_MASK) >> 31 // first index past length
            // second index out of bounds and should be zero
            | (wordIdx2 & NEGATIVE_MASK) >> 30 // second index negative
            | ((words.length - wordIdx2 - 1) & NEGATIVE_MASK) >> 30 // second index past length
            | ((mod64 | (~mod64 + 1)) & NEGATIVE_MASK) >> 31 // edge case: we want to get the full word at wordIdx1.
                                                             // we set word2 to 0, and word1 is set to the word.
            ;

        long word1 = 0;
        long word2 = 0;

        switch (boundsSwitchMask) {
            case 0b00:
                // both items in bounds
                word1 = words[wordIdx1];
                word2 = words[wordIdx2];
                break;
            case 0b01:
                word1 = words[wordIdx1];
                break;
            case 0b10:
                word2 = words[wordIdx2];
                break;
//            case 0b11:
//                word1 = word2 = 0;
        }

        // problem: when shift % 64 == 0. then we will OR both words. We can resolve this with the 0b01 case.
        return word1 << (64 - shift) | word2 >>> shift;
    }

    /**
     * note - this may be negative if bitIdx is before offset
     */
    private int wordIndex(int bitIdx) {
        // = bitIdx / 64
        int relative = bitIdx - offset;
        return relative >> POW2_BITS_PER_WORD;
    }


    public static void main(String[] args) {
//        System.out.println(Long.toBinaryString(-1L << 0));
//        System.out.println(Long.toBinaryString(-1L << 15));
//        System.out.println(Long.toBinaryString(-1L << 63));
//        System.out.println(Long.toBinaryString(-1L << 64));
    }
}

package io.hostilerobot.ceramicrelief.collection.bitset;


/**
 * idea: we have a smaller bitset we can utilize by keeping a "viability window" that can slide around
 * the current maximum capacity is words.length * Long.SIZE
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

    }

    @Override
    public void and(IBitSet other) {
        if(other.getClass() == OffBitSet.class) {
            // optimize so we don't just call wordAt a bunch of times.
            OffBitSet otherBs = (OffBitSet) other;


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

    @Override
    public long wordAt(int index) {
        // word starts at index and goes till index + 63
        int wordIdx1 = wordIndex(index);
        int wordIdx2 = wordIdx1 + 1; // the next word

        int boundsSwitchMask =
            // testing if index is already negative
              (wordIdx1 & NEGATIVE_MASK) >> 31
            | (wordIdx2 & NEGATIVE_MASK) >> 30
            | ((words.length - wordIdx1) & NEGATIVE_MASK) >> 29
            | ((words.length - wordIdx2) & NEGATIVE_MASK) >> 28;

        switch (boundsSwitchMask) {
            case 0b00:
            case 0b01:
            case 0b10:
            case 0b
        }


        long word1;
        long word2;

        // what happens if the word index starts at -1 and we want to go to 0?
        // lets case it out:
        //   wordIdx1 < 0 -> word1 = 0
        //   wordIdx2 < 0 -> word2 = 0
        //   wordIdx1 >= words.length -> word1 = 0
        //   wordIdx2 >= words.length -> word2 = 0
        //   wordIdx1 -> word1 = words[wordIdx1]
        //   wordIdx2 -> word2 = words[wordIdx2]

        // wordIdx2 < 0 implies wordIdx1 < 0
        // wordIdx1 >= words.length implies wordIdx2 >= words.length
        // is there a way that we can do this more efficiently?
        return 0;
    }

    /**
     * note - this may be negative if bitIdx is before offset
     */
    private int wordIndex(int bitIdx) {
        // = bitIdx / 64
        int relative = bitIdx - offset;
        return relative >> POW2_BITS_PER_WORD;
    }

}

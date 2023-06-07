package io.hostilerobot.ceramicrelief.collection.bitset;

interface IBitSet {
    boolean isEmpty();
    // for fixed capacity bitsets
    boolean canOr(IBitSet other);
    boolean canSet(int index);
    boolean get(int index);
    void set(int index);
    void or(IBitSet other);
    void and(IBitSet other);
    int getMinIndex();
    int getMaxIndex();
    long wordAt(int index); // returns the word that starts at getMinIndex() and ends at (getMinIndex() + 64)
}

package io.hostilerobot.ceramicrelief.util;

/**
 *
 * https://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
 */
public final class Hash {
    private Hash(){}
    public static int hashOrdered(int a, int b) {
        return 31 * a + b;
    }
    public static int hashSymmetric(int a, int b) {
        int ahash = hash(a);
        int bhash = hash(b);
        return ((ahash ^ b) * (bhash ^ a)) + ((ahash * b) ^ bhash) + ((bhash * a) ^ ahash);
    }
    public static int hash(int x) {
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        x = ((x >>> 16) ^ x) * 0x45d9f3b;
        x = (x >>> 16) ^ x;
        return x;
    }

    public static int unhash(int x) {
        x = ((x >>> 16) ^ x) * 0x119de1f3;
        x = ((x >>> 16) ^ x) * 0x119de1f3;
        x = (x >>> 16) ^ x;
        return x;
    }
}

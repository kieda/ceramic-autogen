package io.hostilerobot.ceramicrelief.util;


import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

/**
 *
 * https://stackoverflow.com/questions/664014/what-integer-hash-function-are-good-that-accepts-an-integer-hash-key
 */
public final class Hash {
    private Hash(){}
    public static void main(String[] args) {
        int max = 2000;
        Map<Integer, Integer> collisions1 = new HashMap<>(max * (max + 1)/2);
        Map<Integer, Integer> collisions2 = new HashMap<>(max * (max + 1)/2);
        for(int i = 0; i <= max; i++) {
            for(int j = i; j <= max; j++) {
                int hash1 = hashSymmetric1(i, j);
                int hash2 = hashSymmetric2(i, j);
                if(hash1 != hashSymmetric1(j, i))
                    throw new RuntimeException(i + " " + j + " " + hash1 + " " + hashSymmetric1(j, i));
                if(hash2 != hashSymmetric2(j, i))
                    throw new RuntimeException(i + " " + j);
                BiFunction<Integer, Integer, Integer> increment =
                        (k, v) -> v == null ? 0 : v + 1;
                collisions1.compute(hash1, increment);
                collisions2.compute(hash2, increment);
            }
        }
//        BiConsumer<Integer, Integer> print = (k, v) -> {
//            if(v > 0) System.out.println(k + " " + v);
//        };
        System.out.println(collisions1.values().stream().mapToInt(x -> x).sum());
        System.out.println("==========");
        System.out.println(collisions2.values().stream().mapToInt(x -> x).sum());
    }

    
    /*
    how many collisions are there?
    there are 2^32 total int values. call this k
    number of unique symmetric pairs: k*(k+1)/2
    k total spots
    k*(k-1)/2 collisions
    k+1 / 2 average collisions per bucket
    */
    public static int hashSymmetric1(int a, int b) {
        int ahash = hash(a);
        int bhash = hash(b);
        return ((ahash * b) ^ bhash) + ((bhash * a) ^ ahash);
    }
    public static int hashSymmetric2(int a, int b) {
        int ahash = hash(a);
        int bhash = hash(b);
        return ((ahash ^ b) * (bhash ^ a))
                + ((ahash * b) ^ bhash)
                + ((bhash * a) ^ ahash);
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

    public static long hash(long x) {
        x = (x ^ (x >>> 30)) * (0xbf58476d1ce4e5b9L);
        x = (x ^ (x >>> 27)) * (0x94d049bb133111ebL);
        x = x ^ (x >>> 31);
        return x;
    }
    public static long unhash(long x) {
        x = (x ^ (x >>> 31) ^ (x >>> 62)) * (0x319642b2d24d8ec3L);
        x = (x ^ (x >>> 27) ^ (x >>> 54)) * (0x96de1b173f119089L);
        x = x ^ (x >>> 30) ^ (x >>> 60);
        return x;
    }
}

package io.hostilerobot.ceramicrelief.util.chars;

import java.util.Arrays;

public final class CharArrays {
    private CharArrays() {}
    public static char[] sort(char... in) {
        Arrays.sort(in);
        return in;
    }
    public static <R extends CharSequence> String map(CharFunction<R> mapper,
                                                      char... vals) {
        StringBuilder builder = new StringBuilder();
        for(char c : vals) {
            R result = mapper.apply(c);
            builder.append(result);
        }
        return builder.toString();
    }
}

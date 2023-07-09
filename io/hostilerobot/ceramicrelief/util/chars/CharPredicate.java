package io.hostilerobot.ceramicrelief.util.chars;

public interface CharPredicate {
    boolean test(char c);

    public static CharPredicate from(char match) {
        return c -> c == match;
    }
}

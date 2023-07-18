package io.hostilerobot.ceramicrelief.util.chars;

import java.util.function.Predicate;

public interface CharBiPredicate<T> {
    public boolean test(char c, T t);
    public static <T> CharBiPredicate<T> from(char match) {
        return (c, t) -> c == match;
    }
    public static <T> CharBiPredicate<T> from(CharPredicate match) {
        return (c, t) -> match.test(c);
    }
    public static <T> CharBiPredicate<T> from(char flag, Predicate<T> match) {
        return (c, t) -> c == flag && match.test(t);
    }
}

package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.util.chars.CharArrays;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;

import java.util.Arrays;

public interface AParser<T> {
    public ANode<T> parse(CharSequence cs);
    public int match(CharSequence cs); // -1 if no match, otherwise length of sequence match

    public default boolean ignore() {
        return false;
    }

    public static final int RADIX = 10;
    public static final char[] RESERVED_CHARS = CharArrays.sort(
        ',', '=', '/', '\\', '#', '(', ')', '<', '>', '[', ']', '-', ':'
    );
    static final char[] ESCAPED_CHARS = CharArrays.sort(
        '\\', '[', ']', '-'
    );
    // characters that can't be found in a name
    public static final String RESERVED = CharArrays.map(c -> {
        if(Arrays.binarySearch(ESCAPED_CHARS, c) >= 0) {
            return SmallCharSequence.make('\\', c);
        }
        return SmallCharSequence.make(c);
    }, RESERVED_CHARS) + "\\s";
}

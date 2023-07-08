package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import io.hostilerobot.ceramicrelief.util.chars.CharArrays;
import io.hostilerobot.ceramicrelief.util.chars.SmallCharSequence;

import java.util.Arrays;

public interface AParser<T, V extends ANode<T>> {
    public V parse(CharSequence cs);
    public int match(CharSequence cs); // -1 if no match, otherwise length of sequence match

    public static int skipWhiteSpace(CharSequence cs, int pos) {
        while(pos < cs.length() && Character.isWhitespace(cs.charAt(pos))) {
            pos++;
        }
        return pos;
    }
    public static int skipNumber(CharSequence cs, int pos) {
        char current;
        // include - or + in the number
        if(pos < cs.length() && ((current = cs.charAt(pos)) == '-' || current == '+'))
            pos++;
        while(pos < cs.length() && ((current = cs.charAt(pos)) <= '9') && current >= '0') {
            pos++;
        }
        return pos;
    }
    public static int skipName(CharSequence cs, int pos) {
        if(pos < cs.length()) {
            // skip first char
            char current = cs.charAt(pos);
            if((current <= '9' && current >= '0') || Character.isWhitespace(current)
                || Arrays.binarySearch(RESERVED_CHARS, current) >= 0) {
                // illegal first character. Nothing to skip
                return pos;
            }
            pos++; // first character valid. Increment pos
            while(pos < cs.length()
                    && !Character.isWhitespace(current = cs.charAt(pos))
                    && Arrays.binarySearch(RESERVED_CHARS, current) < 0) {
                // skip till we find an invalid character or string ends
                pos++;
            }
        }
        return pos;
    }


    public static final int RADIX = 10;
    public static final char[] RESERVED_CHARS = CharArrays.sort(
        ',', '.', '=', '/', '\\', '#', '(', ')', '<', '>', '[', ']', '-'
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
        //StringJoiner
          //  "\\s,.=/\\\\#()<>\\[\\]-";
}

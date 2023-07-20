package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AName;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ANameParser implements AParser<CharSequence>{
    // don't allow 0-9 in first char, but allow in subsequent chars

    public static int nameLength(CharSequence cs) {
        int pos = 0;
        if(pos < cs.length()) {
            // check first char - decimals, whitespace, and reseved chars not allowed
            char current = cs.charAt(0);
            if((current >= '0' && current <= '9') || Character.isWhitespace(current)
                    || Arrays.binarySearch(RESERVED_CHARS, current) >= 0) {
                // illegal first character. Nothing to skip
                return 0;
            }
            pos++; // first character valid. Increment pos
            // check the rest of the characters. Stop on whitespace or reserved chars
            while(pos < cs.length()
                    && !Character.isWhitespace(current = cs.charAt(pos))
                    && Arrays.binarySearch(RESERVED_CHARS, current) < 0) {
                // skip till we find an invalid character or string ends
                pos++;
            }
        }

        return pos;
    }

    @Override
    public AName parse(CharSequence cs) {
        int nameLength = nameLength(cs);
        CharSequence name = cs.subSequence(0, nameLength);
        return new AName(name);
    }

    @Override
    public int match(CharSequence cs) {
        int nameLength = nameLength(cs);
        return nameLength == 0 ? -1 : nameLength; // empty names are treated as invalid
    }
}

package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AName;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ANameParser implements AParser<CharSequence, AName>{
    // don't allow 0-9 in first char, but allow in subsequent chars
    public static Pattern NAME_PAT = Pattern.compile(
            "^(?:[^0-9" + AParser.RESERVED + "])(?:[^" + AParser.RESERVED + "]*)");

    @Override
    public AName parse(CharSequence cs) {
        int endName = AParser.skipName(cs, 0);
        CharSequence name = cs.subSequence(0, endName);
        return new AName(name);
    }

    @Override
    public int match(CharSequence cs) {
        Matcher m = NAME_PAT.matcher(cs);
        if(!m.lookingAt())
            return -1;
        return m.toMatchResult().end();
    }
}

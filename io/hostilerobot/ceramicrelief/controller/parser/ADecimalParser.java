package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ADecimal;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ADecimalParser implements AParser<Double> {

    @Override
    public ADecimal parse(CharSequence cs) {
        double val = Double.parseDouble(String.valueOf(cs));
        ADecimal dec = new ADecimal(val);
        return dec;
    }
    private final static Pattern DOUBLE_PAT = Pattern.compile(
            "^[+-]?([0-9]*)(?:\\.([0-9]+))?([eE][0-9]+)?");
    @Override
    public int match(CharSequence cs) {
        Matcher m = DOUBLE_PAT.matcher(cs);
        if(!m.lookingAt())
            return -1;
        MatchResult res = m.toMatchResult();
        // input [+-](\\.)? is invalid
        // do this by checking if both capture groups have 0 size
        if(((res.end(1) - res.start(1)) | (res.end(2) - res.start(2))) == 0) {
            return -1;
        }
        return res.end();
    }

    public static void main(String[] args) {
        //                                    0123456
        Matcher m = DOUBLE_PAT.matcher("-");
        m.find();
        MatchResult res = m.toMatchResult();
        System.out.println(res.start(1)
         + " " + res.end(1) +
                " " + res.start(2) + " " + res.end(2));
        System.out.println(((res.end(1) - res.start(1)) | (res.end(2) - res.start(2))) == 0);
    }
}

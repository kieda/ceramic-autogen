package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AQuotient;
import org.apache.commons.math.fraction.Fraction;

import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AQuotientParser implements AParser<Fraction, AQuotient> {

    @Override
    public AQuotient parse(CharSequence cs) {
        // end of the numerator, exclusive
        int endNumerator = AParser.skipNumber(cs, 0);
        int numerator = Integer.parseInt(cs, 0, endNumerator, AParser.RADIX);
        int dividePos = AParser.skipWhiteSpace(cs, endNumerator);
        if(dividePos < cs.length() && cs.charAt(dividePos) == '/') {
            // get the denominator too.
            int startDenominator = AParser.skipWhiteSpace(cs, dividePos + 1);
            int endDenominator = AParser.skipNumber(cs, startDenominator);
            if(startDenominator != endDenominator) {
                int denominator = Integer.parseInt(cs, startDenominator, endDenominator, AParser.RADIX);
                return new AQuotient(numerator, denominator);
            }
        }
        return new AQuotient(numerator, 1);
    }

    private final static Pattern QUOTIENT_PAT = Pattern.compile(
        "^([+-])?[0-9]+\\s*(\\/\\s*[0-9]+)?", Pattern.MULTILINE
    );

    @Override
    public int match(CharSequence cs) {
        Matcher m = QUOTIENT_PAT.matcher(cs);
        if(!m.lookingAt())
            return -1;
        return m.toMatchResult().end();
    }
}

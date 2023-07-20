package io.hostilerobot.ceramicrelief.controller.parser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.junit.jupiter.api.Assertions.*;

class ADecimalParserTest {
    private static ADecimalParser parser;
    @BeforeAll
    static void setUpTest() {
        // we should be able to use the same comment parser multiple times
        // with the same results
        parser = new ADecimalParser();
    }

    @AfterAll
    static void tearDown() {
        parser = null;
    }

    public static class ValidDecimalArguments extends ParserTestArguments<Double> {
        {
            addStrippedTest("123.456", 123.456);
            addStrippedTest("123.456 \t", 123.456);
            addStrippedTest("-123.456 ", -123.456);
            addStrippedTest("-  123.456 ", -123.456);
            addStrippedTest("-  123. ", -123.0);
            addStrippedTest("-  123.0 ", -123.0);
            addStrippedTest("+  123. ", +123.0);
            addStrippedTest("+ # this is a comment because we have whitespace\n 123       ", 123.);
            addStrippedTest("- # this is a comment because we have whitespace\n .5", -.5);
            addStrippedTest("- # this is a comment because we have whitespace\n 0.5\r\n", -.5);
            addStrippedTest("- # this is a comment because we have whitespace\n 0.0 \n\n", -0.0);
            addStrippedTest("- # this is a comment because we have whitespace\n 0    \t", -0.0);
            addStrippedTest("- # this is a comment because we have whitespace\n .0 \t", -0.0);
            addStrippedTest("- # this is a comment because we have whitespace\n 0.0   ", -0.0);
            addStrippedTest("1", 1.0);
            addStrippedTest("2.0", 2.0);
            addStrippedTest("3.", 3.0);
            addStrippedTest(".4", .4);
            addStrippedTest("-1", -1.0);
            addStrippedTest("-2.0", -2.0);
            addStrippedTest("-3.", -3.0);
            addStrippedTest("-.4", -.4);
            addStrippedTest("+1", 1.0);
            addStrippedTest("+2.0", 2.0);
            addStrippedTest("+3.", 3.0);
            addStrippedTest("+.4", .4);
            int parseLen123 = "123.456".length();
            addTest("123.456 .789", 123.456, parseLen123);
            addTest("123.456+789", 123.456, parseLen123);
            addTest("123.456-789", 123.456, parseLen123);
            addTest("123.456 -789", 123.456, parseLen123);
            addTest("1. 123", 1.0, 2);
            int parseLenComment = "+  #comment lol\n4".length();
            addTest("+  #comment lol\n4 some other value", 4.0, parseLenComment);
            addTest("+  #comment lol\n4-123", 4.0, parseLenComment);
            addTest("-  #comment lol\n4-123", -4.0, parseLenComment);
            addTest("-  #comment lol\n4+123", -4.0, parseLenComment);
            addTest("+  #comment lol\n4=myVal", 4.0, parseLenComment);
            addTest("+  #comment lol\n4,listItem", 4.0, parseLenComment);
            addTest("+  #comment lol\n4) end list", 4.0, parseLenComment);
        }
    }

    public static class InvalidDecimalArguments extends ParserTestArguments<Double> {
        {
            addTest(" 123.456", -1);
            addTest("", -1);
            addTest("NaN", -1);
            addTest(".", -1);
            addTest(". 123", -1);
            addTest("+", -1);
            addTest("-", -1);
            final boolean[] TRUE_FALSE = new boolean[]{true, false};
            final String[] PLUS_MINUS = new String[]{"+", "-"};
            for(String sign : PLUS_MINUS) {
                for(boolean sepFirst : TRUE_FALSE) {
                    String firstPart = (sepFirst ? "." + sign : sign + ".");
                    if(sepFirst) {
                        for (boolean addNum : TRUE_FALSE) {
                            String input = firstPart + (addNum ? "123" : "");
                            addTest(input, -1);
                        }
                    } else
                        addTest(firstPart, -1);
                }
            }
            addTest(".qwert", -1);
            addTest("123.qwert", -1);
            addTest("123qwert", -1);
            addTest("123.456qwert", -1);
            addTest("123..456", -1);
            addTest("123.45.6", -1);
            for(String sign1 : PLUS_MINUS)
                for(String sign2 : PLUS_MINUS)
                    for(boolean addNum : TRUE_FALSE)
                        for(boolean addSpace : TRUE_FALSE)
                            addTest(sign1 + (addSpace ? " " : "") + sign2 + (addNum ? "123" : ""), -1);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(InvalidDecimalArguments.class)
    void testInvalidDecimals(String input,int expectedMatchLen) {
        int matchLength = parser.match(input);
        assertEquals(expectedMatchLen, matchLength);
    }

    @ParameterizedTest
    @ArgumentsSource(ValidDecimalArguments.class)
    void testValidDecimals(String input, double expected, int expectedMatchLen) {
        int matchLength = parser.match(input);
        assertEquals(expectedMatchLen, matchLength);
        if(matchLength >= 0) {
            assertEquals(expected, parser.parse(input).getValue());
        }
    }
}
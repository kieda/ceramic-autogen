package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AQuotient;
import org.apache.commons.math.fraction.Fraction;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.junit.jupiter.api.Assertions.*;

class AQuotientParserTest {
    public static AQuotientParser parser;

    public static class QuotientArguments extends ParserTestArguments<Fraction> {
        public static final int LEADING = 1;
        public static final int TRAILING = 2;
        static boolean hasLeading(int flag) {
            return (flag & LEADING) != 0;
        }
        static boolean hasTrailing(int flag) {
            return (flag & TRAILING) != 0;
        }

        static void appendSpace(int seq, int idx, StringBuilder sb) {
            int flag = seq << idx;
            if((seq & flag) != 0) sb.append(' ');
        }

        static String internStringWhitespace(String base, int seq) {
            return internStringWhitespace(base, seq, LEADING | TRAILING);
        }
        static String internStringWhitespace(String base, int seq, int flags) {
            StringBuilder result = new StringBuilder();
            boolean hasLeading = hasLeading(flags);
            // ab = 2
            // hasLeading = false
            // "a( )?b"
            //    0
            // hasTrailing = true
            // "a( )?b( )?"
            //    0    1
            // "a( )?"
            //    0
            for(int charidx=0; charidx < base.length(); charidx++) {
                if(hasLeading) {
                    appendSpace(seq, charidx, result);
                } else if(charidx > 0) {
                    appendSpace(seq, charidx - 1, result);
                }

                result.append(base.charAt(charidx));
            }
            if(hasTrailing(flags) && !(base.isEmpty() && !hasLeading)) {
                int lastPos = base.length() - (hasLeading ? 0 : 1);
                // base is empty and we don't have leading
                appendSpace(seq, lastPos, result);
            }
            return result.toString();
        }
        static int countWaysToIntern(String base, int flags) {
            int toadd = Integer.bitCount(flags & 0b11);
            // base.length = 1
            int logPossibilities = base.length() - 1 + toadd;
            if(logPossibilities < 0)
                return 0;
            return 1 << (base.length() - 1 + toadd);
        }
        static int countWaysToIntern(String base) {
            return countWaysToIntern(base, LEADING | TRAILING);
        }

        /**
         * requirements: test case must be valid in terms of parsing, and cannot begin or end with a comment
         */
        public void addStrippedTest(String fraction, Fraction expected) {
            String strippedLeading = fraction.stripLeading();
            // if we have any leading whitespace, then we would expect match() to result in -1
            // though we may be able to parse it successfully.
            int expectedMatch;
            if(strippedLeading.length() < fraction.length()) {
                expectedMatch = -1;
            } else {
                expectedMatch = fraction.stripTrailing().length();
            }
            addTest(fraction, expected, expectedMatch);
        }
    }

    /**
     * tests taking the form "([-+])?3[+- ]1/2", where whitespace may occur anywhere.
     */
    public static class TwoPartQuotientArguments extends QuotientArguments {
        {
            // test [+-]?3[+-]1/2
            String[] INTPARTS = {"-3", "+3", "3"};
            Fraction THREE = new Fraction(3, 1);
            Fraction[] INTFRAC = {THREE.negate(), THREE, THREE};
            String[] FRACPARTS = {"+1/2", "-1/2"};
            Fraction[] FRACFRAC = {Fraction.ONE_HALF, Fraction.ONE_HALF.negate()};
            for(int intIdx = 0; intIdx < INTPARTS.length; intIdx++) {
                for(int fracIdx = 0; fracIdx < FRACPARTS.length; fracIdx++) {
                    String fractionString = INTPARTS[intIdx] + FRACPARTS[fracIdx];
                    Fraction fractionResult = INTFRAC[intIdx].add(FRACFRAC[fracIdx]);
                    for(int i = 0; i < countWaysToIntern(fractionString); i++) {
                        addStrippedTest(internStringWhitespace(fractionString, i), fractionResult);
                    }
                }
            }

            // test [+-]?3 1/2
            for(int intIdx = 0; intIdx < INTPARTS.length; intIdx++) {
                // 1/2 is the only valid part
                String firstPart = INTPARTS[intIdx];
                String secondPart = "1/2";

                // -3 < 0 => -1
                // 3 > 0 => 1
                // 3 == 0 => 0
                int sign = Integer.signum(INTFRAC[intIdx].compareTo(Fraction.ZERO));
                // [3,-3] + ([-1, 1]) * 1/2
                Fraction fractionResult = INTFRAC[intIdx].add(Fraction.ONE_HALF.multiply(sign));
                // don't count/use extra combinations for spaces that are between firstPart and secondPart
                int firstPartWays = countWaysToIntern(firstPart, LEADING);
                int secondPartWays = countWaysToIntern(secondPart, TRAILING);
                for(int i = 0; i < firstPartWays; i++) {
                    for(int j = 0; j < secondPartWays; j++) {
                        // just count the combinations for the first way and second way
                        String testString = internStringWhitespace(firstPart, i, LEADING) + " "
                                + internStringWhitespace(secondPart, j, TRAILING);
                        addStrippedTest(testString, fractionResult);
                    }
                }
            }
        }
    }

    /**
     * tests taking the form "([-+])?1/2", where whitespace may occur anywhere.
     * also tests "([+-])?123" where whitespace may occur anywhere
     */
    public static class OnePartQuotientArguments extends QuotientArguments {
        {
            String[] INTPARTS = new String[]{"-", "+", ""};
            Fraction[] INTFRAC = new Fraction[]{Fraction.MINUS_ONE, Fraction.ONE, Fraction.ONE};
            String[] FRACPARTS = new String[]{"1/2"};
            Fraction[] FRACFRAC = new Fraction[]{Fraction.ONE_HALF};
            for(int intIdx = 0; intIdx < INTPARTS.length; intIdx++) {
                for(int fracIdx = 0; fracIdx < FRACPARTS.length; fracIdx++) {
                    String fractionString = INTPARTS[intIdx] + FRACPARTS[fracIdx];
                    Fraction fractionResult = INTFRAC[intIdx].multiply(FRACFRAC[fracIdx]);
                    for(int i = 0; i < countWaysToIntern(fractionString); i++) {
                        addStrippedTest(internStringWhitespace(fractionString, i), fractionResult);
                    }
                }
            }

            addStrippedTest("+ 312", new Fraction(312, 1));
            addStrippedTest("+312", new Fraction(312, 1));
            addStrippedTest(" +312", new Fraction(312, 1));
            addStrippedTest(" + \t312", new Fraction(312, 1));
            addStrippedTest("- 312", new Fraction(-312, 1));
            addStrippedTest("-312", new Fraction(-312, 1));
            addStrippedTest(" -312", new Fraction(-312, 1));
            addStrippedTest(" - 312", new Fraction(-312, 1));
            addStrippedTest("  312", new Fraction(312, 1));
        }
    }



    public static class BasicQuotientArguments extends QuotientArguments {

        {
            addStrippedTest("3/4", Fraction.THREE_QUARTERS);
            addStrippedTest("+3/4", Fraction.THREE_QUARTERS);
            addStrippedTest("-3/4", Fraction.THREE_QUARTERS.negate());
            addStrippedTest("""
                    3/ # here is a comment!
                    4
                    """, Fraction.THREE_QUARTERS);
            addTest("""
                    3 # three needs its own comment
                    / # here is a comment!
                    4 # and another
                    """, Fraction.THREE_QUARTERS, "3 # three needs its own comment\n/ # here is a comment!\n4".length());
            addStrippedTest("""
                    3 \t# three needs its own comment 
                    /4""", Fraction.THREE_QUARTERS);
            addTest("""
                       # just some whitespace
                     - \t # negativeeee
                    1 # three needs its own comment
                    /4""", new Fraction(-1, 4), -1); // starts with whitespace -> no match
            addTest("""
                    # starting with a comment
                    -1/4 # comment end""", new Fraction(-1, 4), -1); // starts with comment -> no match
            addStrippedTest("123 15/  209", new Fraction(25722, 209));
            addStrippedTest("-0 0/15", Fraction.ZERO);
            addStrippedTest("+0 0/15", Fraction.ZERO);
            addStrippedTest("+0 3/15", Fraction.ONE_FIFTH);
            addStrippedTest("-5007 0/15", new Fraction(-5007, 1));
        }
    }
    @BeforeAll
    static void setUpTest() {
        // we should be able to use the same comment parser multiple times
        // with the same results
        parser = new AQuotientParser();
    }

    @AfterAll
    static void tearDown() {
        parser = null;
    }

    @ParameterizedTest
    @ArgumentsSource(OnePartQuotientArguments.class)
    void testParseOnePartQuotients(String input, Fraction expected, int expectedMatchLen) {
        assertEquals(expectedMatchLen, parser.match(input));
        AQuotient ac = parser.parse(input);
        assertEquals(expected, ac.getValue());
    }

    @ParameterizedTest
    @ArgumentsSource(TwoPartQuotientArguments.class)
    void testParseTwoPartQuotients(String input, Fraction expected, int expectedMatchLen) {
        assertEquals(expectedMatchLen, parser.match(input));
        AQuotient ac = parser.parse(input);
        assertEquals(expected, ac.getValue());
    }

    @ParameterizedTest
    @ArgumentsSource(BasicQuotientArguments.class)
    void testParseManualQuotients(String input, Fraction expected, int expectedMatchLen) {
        assertEquals(expectedMatchLen, parser.match(input));
        AQuotient ac = parser.parse(input);
        assertEquals(expected, ac.getValue());
    }
}
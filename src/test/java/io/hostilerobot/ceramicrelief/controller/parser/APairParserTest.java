package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.*;
import org.apache.commons.collections4.iterators.PermutationIterator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class APairParserTest {
    private static String whitespaceIntern(int mask, int length, String... parts) {
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < length; i++) {
            sb.append(parts[i]);
            if((mask & (1 << i)) != 0) {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
    private static String whitespaceIntern(int mask, String... parts) {
        return whitespaceIntern(mask, parts.length, parts);
    }
    private static int waysToCountIntern(int partsLength) {
        // {} -> {} // 0
        // {"a"} -> {"a", "a "} // 0,1
        // {"a", "b"} -> {"ab", "a b", "ab ", "a b "} // 0,1,2,3

        return partsLength == 0 ? 0 : 1 << partsLength;
    }

    private static <T> void addInternTests(ParserTestArguments<T> args, T result, String... parts) {
        addInternTests(args, result, true, parts);
    }
    private static <T> void addInternTests(ParserTestArguments<T> args, T expected, boolean countLast, String... parts) {
        int ways = waysToCountIntern(parts.length);
        for(int mask = 0; mask < ways; mask++) {
            String input = whitespaceIntern(mask, parts);
            int expectedLength;
            if(countLast) {
                expectedLength = input.stripTrailing().length();
            } else {
                // if we have a comment or some other string at the end, e.g "{", "hello", "=", "world", "}", "# my own comment!"
                // we permit the user to specify that the expected length should not include the last part.
                expectedLength = whitespaceIntern(mask, parts.length - 1, parts).stripTrailing().length();
            }

            args.addTest(input, expected, expectedLength);
        }
    }

    public static class StringDecimalArguments extends ParserTestArguments<Pair<String, Double>> {
        {
            addInternTests(this, new Pair<>("hello", 123.0), "hello", "=", "123");
            addInternTests(this, new Pair<>("world", -.678), "{", "world", "=", "-", ".678", "}");
            addStrippedTest("""
                    { # begin pair
                      hello123 # key
                        = # this is an equals
                      55 # val
                    }""", new Pair<>("hello123", 55.0));

            addInternTests(this, new Pair<>("groovy", 0.), false,"groovy", "=", "0", "#my comment");
            addInternTests(this, new Pair<>("yappin", -1.), false, "{", "yappin", "=", "-", "1.", "}", "#my comment");
            addInternTests(this, new Pair<>("g", 5.), false,"g", "=", "5", "))lol");
            addInternTests(this, new Pair<>("b", 6.), false,"b", "=", "6", " lol");
            // todo empty strings on "=1", "{=2}", "name=", "{name=}", etc. Could be done by adding a default (empty) parser
            //   they currently do different things based on group or raw. Maybe we just have both of them error out?
        }
    }

    private static final ANameParser NAME_PARSER = new ANameParser();
    private static final ADecimalParser DECIMAL_PARSER = new ADecimalParser();
    private static APairParser<CharSequence, Double> NAME_TO_DECIMAL = new APairParser<>(
        List.of(NAME_PARSER), List.of(DECIMAL_PARSER),
        List.of(NAME_PARSER), List.of(DECIMAL_PARSER)
    );
    @ParameterizedTest
    @ArgumentsSource(StringDecimalArguments.class)
    public void testStringDecimals(String input, Pair<String, Double> expected, int expectedMatch) {
        assertEquals(expectedMatch, NAME_TO_DECIMAL.match(input));
        APair<CharSequence, Double> actual = NAME_TO_DECIMAL.parse(input);
        AName name = (AName) actual.getValue().getKey();
        ADecimal decimal = (ADecimal) actual.getValue().getVal();
        assertEquals(expected.getKey(), String.valueOf(name.getValue()));
        assertEquals(expected.getVal(), decimal.getValue());
    }
    private sealed interface PairTree{}
    private record VName(String name) implements PairTree{}
    private record VDecimal(double val) implements PairTree{}
    private record VPair(PairTree first, PairTree second) implements PairTree{}
    private static VName of(String name){
        return new VName(name);
    }
    private static VDecimal of(double decimal) {
        return new VDecimal(decimal);
    }
    private static VPair of(PairTree first, PairTree second) {
        return new VPair(first, second);
    }


    public static class NestedPairArguments extends ParserTestArguments<PairTree> {
        {
            // "a = b = 123 = c" = "{a = {b = {123 = c}}}
            addInternTests(this,
                    of(of("a"), of(of("b"), of(of(123.0), of("c")))),
                    "a", "=", "b", "=", "123", "=", "c");
            // {{ a = b } = 123 } = c
            addInternTests(this,
                    of(of(of(of("a"), of("b")), of(123.0)), of("c")),
                    "{", "{", "a", "=", "b", "}", "=", "123", "}", "=", "c");
        }
    }

    private static APairParser<Object, Object> NAMES_DECIMALS_AND_PAIRS;
    static {
        List<AParser<? extends Object>> parsers = new ArrayList<>();
        parsers.add(null);
        parsers.add(NAME_PARSER);
        parsers.add(DECIMAL_PARSER);
        NAMES_DECIMALS_AND_PAIRS = new APairParser<>(parsers, parsers, parsers, parsers);
        parsers.set(0, NAMES_DECIMALS_AND_PAIRS);
    }


    private void assertTreeEquals(PairTree expected, Object actual) {
        assertNotNull(actual);
        switch(expected) {
            case VDecimal(double expectedVal) -> {
                assertEquals(actual.getClass(), ADecimal.class);
                ADecimal actualDecimal = (ADecimal) actual;
                assertEquals(expectedVal, actualDecimal.getValue());
            }
            case VName(String expectedName) -> {
                assertTrue(actual instanceof AName, actual + "(" + actual.getClass() + ")" + " !instanceof " + AName.class);
                AName actualChars = (AName) actual;
                assertEquals(expectedName, String.valueOf(actualChars.getValue()));
            }
            case VPair(PairTree expectedKey, PairTree expectedVal) -> {
                assertEquals(APair.class, actual.getClass());
                APair<Object, Object> actualPair = (APair<Object, Object>) actual;
                assertNotNull(actualPair.getValue());
                assertTreeEquals(expectedKey, actualPair.getValue().getKey());
                assertTreeEquals(expectedVal, actualPair.getValue().getVal());
            }
        }
    }
    @ParameterizedTest
    @ArgumentsSource(NestedPairArguments.class)
    public void testPairs(String input, PairTree expected, int expectedMatch) {
        System.out.println(input + " " + input.length());
        assertEquals(expectedMatch, NAMES_DECIMALS_AND_PAIRS.match(input));
        assertTreeEquals(expected, NAMES_DECIMALS_AND_PAIRS.parse(input));
    }
}
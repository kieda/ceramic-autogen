package io.hostilerobot.ceramicrelief.controller.parser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.Map;

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

    public static class TestDecimalArguments extends ParserTestArguments<Double> {
        private Map<String, String> validTests = Map.of(
            "123.456", "123.456",
            "-123.456", "-123.456",
            "  \t -1 asdf -1  ", "-1"
        );

        {
            for(Map.Entry<String, String> test : validTests.entrySet()) {
                int matchIdxStart = test.getKey().indexOf(test.getValue());
                int matchIdxEnd = matchIdxStart + test.getValue().length();
                test.getKey().substring(matchIdxStart, matchIdxEnd);
                addTest(test.getKey(), Double.parseDouble(test.getValue()), matchIdxEnd);
            }
        }
    }


    @ParameterizedTest
    @ArgumentsSource(TestDecimalArguments.class)
    void match(String input, int expected) {
        int matchLength = parser.match(input);
        assertEquals(expected, matchLength);
    }


}
package io.hostilerobot.ceramicrelief.controller.parser;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.junit.jupiter.api.Assertions.*;

class ANameParserTest {
    static class TestNameArguments extends ParserTestStringArguments {
        {
            addTest("hello💪", "hello💪");
            addTest("cool😎name\t   ", "cool😎name");
            addTest("x", "x");
            addTest("x  ", "x");
            addTest("x_y", "x_y");
            addTest("x_y   123", "x_y");
            addTest("🚽5   123", "🚽5");
            addTest("r12345  lol", "r12345");

            // invalid names
            addTest("1delta ", -1);
            addTest("-mojo", -1);
            addTest(" spacetest", -1);
            addTest(" ", -1);
            addTest("", -1);
        }
    }

    private static ANameParser parser;

    @BeforeAll
    static void setUp() {
        parser = new ANameParser();
    }
    @AfterAll
    static void tearDown() {
        parser = null;
    }

    @ParameterizedTest
    @ArgumentsSource(TestNameArguments.class)
    public void testValidNames(String input, String expected, int expectedMatch) {
        int match = parser.match(input);
        assertEquals(expectedMatch, match);
        if(expectedMatch >= 0)
            assertEquals(expected, String.valueOf(parser.parse(input).getValue()));
    }
}
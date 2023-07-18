package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AComment;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;


import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;
import static io.hostilerobot.ceramicrelief.controller.parser.ParserTests.*;

class ACommentParserTest {
    private static ACommentParser parser;

    public static class TestCommentArguments extends ParserTestStringArguments {
        {
            addTest("""
                        # hello world   
                        this is a test
                        """, "# hello world");
            addTest("#\nasdf", "#");
            addTest("\t \t#\nasdf", "#", -1);
            addTest("""
                    string1
                    # comment 1 \t 
                    # comment 2 \t\t
                    string2
                    """, "# comment 1 \t", -1);

            // tests where parsing would fail but match would return -1
            addTest("no comment here!", -1);
            addTest("", -1);

            addExceptionTest("no comment here!", StringIndexOutOfBoundsException.class, "Range \\[.*\\) out of bounds for length \\d+");
            addExceptionTest("", StringIndexOutOfBoundsException.class, "Range \\[.*\\) out of bounds for length \\d+");
            addExceptionTest(null, NullPointerException.class);
        }
    }

    @BeforeAll
    static void setUpTest() {
        // we should be able to use the same comment parser multiple times
        // with the same results
        parser = new ACommentParser();
    }

    @AfterAll
    static void tearDown() {
        parser = null;
    }

    @ParameterizedTest
    @ArgumentsSource(TestCommentArguments.class)
    void testParse(String input, String expected) {
        // have a series of strings mapping to output string
        // if output string is null, then
        AComment ac = parser.parse(input);
        assertEquals(expected, ac.getValue());
    }

    @ParameterizedTest
    @ArgumentsSource(TestCommentArguments.class)
    void match(String input, int expected) {
        int matchLength = parser.match(input);
        assertEquals(expected, matchLength);
    }

    @Test
    void testNullMatch() {
        assertThrows(NullPointerException.class, () -> parser.match(null));
    }

    @ParameterizedTest
    @ArgumentsSource(TestCommentArguments.class)
    void exceptionParse(String input, Class<? extends Throwable> expectedThrown, String expectedErrorMessage) {
        Throwable thrown = assertThrows(expectedThrown, () -> parser.parse(input));
        if(expectedErrorMessage != null)
            assertTrue(thrown.getMessage().matches(expectedErrorMessage), thrown.getMessage());
    }
}
package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ASectionName;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.nio.CharBuffer;

import static org.junit.jupiter.api.Assertions.*;

class ASectionNameParserTest {
    private static ASectionNameParser parser;

    @BeforeAll
    static void setUpTest() {
        parser = new ASectionNameParser();
    }

    @AfterAll
    static void tearDownTest() {
        parser = null;
    }

    public static class TestSectionNameArguments extends ParserTestStringArguments{
        {
            // various valid tests
            addStrippedTest("hello:", "hello");
            addStrippedTest("h:", "h");
            addStrippedTest(":", "");

            addStrippedTest(" buff💪section : ", "buff💪section");
            addStrippedTest("""
                     \t section😱withNewLine 
                        :  
                    """, "section😱withNewLine");
            addTest(": : : lol :", "",  1);
            addTest("::::::", "",  1);
            addTest("m123y_own_$$^name5 : 123 + 13/ 5", "m123y_own_$$^name5", "m123y_own_$$^name5 :".length());
            addTest("""
                        # so many comments 😭
                     \t testingComment🤩  # a comment is allowed because we allow whitespace 📚
                        :  # another comment ☕️
                    """, "testingComment🤩", -1);
        }
    }

    public static class TestMismatchSectionNameArguments extends ParserTestStringArguments{
        {
            // various invalid tests
            addTest("hell o:", -1);
            addTest("hell o :", -1);
            addTest("hell o :", -1);
            addTest("", -1);
            addTest("1delta : ", -1);
            addTest("invalid-test:", -1);
            addTest("jokes 123 :", -1);
            addTest("jokes 123:", -1);
            addTest("nameButNoColon", -1);
            addTest("helloWorld123", -1);
            addTest("test test test", -1);
        }
    }

    @ParameterizedTest
    @ArgumentsSource(TestMismatchSectionNameArguments.class)
    void testMatchSectionName(String input, int expectedMatch) {
        int match = parser.match(input);
        assertEquals(expectedMatch, match);
    }

    @ParameterizedTest
    @ArgumentsSource(TestSectionNameArguments.class)
    void testParseSectionName(String input, String expected, int expectedMatch) {
        int match = parser.match(input);
        assertEquals(expectedMatch, match);
        ASectionName sectionName = parser.parse(input);
        if(sectionName.getValue() != null && !(sectionName.getValue() instanceof String)) {
            assertEquals(expected, String.valueOf(sectionName.getValue()));
        } else {
            assertEquals(expected, sectionName.getValue());
        }
    }
}
package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.AWhitespace;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import static org.junit.jupiter.api.Assertions.*;

class AWhitespaceParserTest {
    public static class TestWhitespaceArguments extends ParserTestArguments<AWhitespace> {
        public void addWhitespaceTest(String str) {
            int lengthParsed;
            if(str.isEmpty())
                lengthParsed = 0;
            else {
                String stripped = str.stripLeading();
                int diff = str.length() - stripped.length();
                lengthParsed = diff == 0 ? -1 : diff;
            }
            addTest(str, lengthParsed);
        }
        {
            addWhitespaceTest("  \r\n\t 123 ");
            addWhitespaceTest(" a");
            addWhitespaceTest("a");
            addWhitespaceTest("");
            addWhitespaceTest("  ");

            addTest("\u0009\n\f\r\u001c\u001d\u001e\u001f\u000b\u0020\u2000\u2001\u2002\u2003\u2004\u2005\u2006\u2008\u2009\u200a\u205f\u3000abc", 22);
            // test that non-breaking whitespace will not count as whitespace
            // '\u00A0', '\u2007', '\u202F'
            addTest(" \u00a0   hello", 1);
            addTest("  \u2007   hello", 2);
            addTest("   \u202f   hello", 3);
        }
    }

    private static AWhitespaceParser parser;
    @BeforeAll
    static void setUp() {
        parser = new AWhitespaceParser();
    }
    @AfterAll
    static void tearDown() {
        parser = null;
    }

    @ParameterizedTest
    @ArgumentsSource(TestWhitespaceArguments.class)
    public void testWhitespace(String input, int expectedMatch) {
        assertEquals(expectedMatch, parser.match(input));
        assertEquals(AWhitespace.getInstance(), parser.parse(input));
    }

    @Test
    public void testWhitespaceInstance() {
        // assert size is zero and we ignore this node
        assertEquals(0, AWhitespace.getInstance().size());
        assertTrue(AWhitespace.getInstance().ignore());
    }
}
package io.hostilerobot.ceramicrelief.controller.parser;

public class ParserTestStringArguments extends ParserTestArguments<String> {
    /**
     * adds a test, expected match length is the string length
     * @param input input to be passed to test
     * @param expected expected string result
     */
    public void addTest(String input, String expected) {
        addTest(input, expected, expectedMatchLength(expected));
    }

    private static int expectedMatchLength(String expectedMatch) {
        if(expectedMatch == null)
            return -1;
        else
            return expectedMatch.length();
    }
}

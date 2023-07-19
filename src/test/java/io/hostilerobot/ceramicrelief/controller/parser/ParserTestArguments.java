package io.hostilerobot.ceramicrelief.controller.parser;

import org.apache.commons.math.fraction.Fraction;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Usage:
 * MyParserTestArguments extends ParserTestArguments<Foo> {
 *     {
 *         addTest("testString1", new Foo(), 3);
 *         addTest("unparsableString1", null, -1);
 *         addExceptionTest("
 *     }
 * }
 * @param <T>
 */
public class ParserTestArguments<T> implements ArgumentsProvider {
    private List<ParserTestCase> testCases = new ArrayList<>();

    public void addTest(String input, int expectedMatch) {
        testCases.add(new ParserTestCase.ValidTestCase<>(input, null, expectedMatch));
    }

    public void addTest(String input, T expectedParse, int expectedMatch) {
        testCases.add(new ParserTestCase.ValidTestCase<>(input, expectedParse, expectedMatch));
    }

    /**
     * this is used for test cases that might have leading or trailing whitespace that can be parsed OK
     * but will still result in a match of -1 since we're not directly looking at the item.
     *
     * requirements: test case must be valid in terms of parsing, and cannot begin or end with a comment
     */
    public void addStrippedTest(String input, T expected) {
        String strippedLeading = input.stripLeading();
        // if we have any leading whitespace, then we would expect match() to result in -1
        // though we may be able to parse it successfully.
        int expectedMatch;
        if(strippedLeading.length() < input.length()) {
            expectedMatch = -1;
        } else {
            expectedMatch = input.stripTrailing().length();
        }
        addTest(input, expected, expectedMatch);
    }

    public <E> void addExceptionTest(String input, Class<? extends Throwable> throwable, String thrownErrorMessage, E additionalExceptionInfo) {
        testCases.add(new ParserTestCase.ExceptionTestCase<>(input, throwable, thrownErrorMessage, additionalExceptionInfo));
    }
    public void addExceptionTest(String input, Class<? extends Throwable> throwable, String thrownErrorMessage) {
        addExceptionTest(input, throwable, thrownErrorMessage, null);
    }
    public void addExceptionTest(String input, Class<? extends Throwable> throwable) {
        addExceptionTest(input, throwable, null);
    }

    public ParserTestCase getTestCase(int index) {
        return testCases.get(index);
    }
    public int getSize() {
        return testCases.size();
    }

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) throws Exception {
        Method targetMethod = context.getRequiredTestMethod();
        AParserTestType type = AParserTestType.getTestType(targetMethod);
        if(type == null) {
            throw new IllegalArgumentException("This class cannot pass arguments to target method " + targetMethod);
        }

        return IntStream.range(0, getSize())
                .mapToObj(idx -> {
                    ParserTestCase testCase = getTestCase(idx);
                    return type.canTarget(testCase) ? type.getArgs(testCase) : null;
                }).filter(Objects::nonNull);
    }
}

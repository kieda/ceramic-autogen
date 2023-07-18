package io.hostilerobot.ceramicrelief.controller.parser;

import org.apache.commons.lang3.ClassUtils;
import org.junit.jupiter.params.provider.Arguments;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

public sealed interface AParserTestType {
    Class<?>[] getSignature();
    default boolean isParamMatch(Parameter[] params) {
        Class<?>[] paramClasses = Arrays.stream(params).map(param -> param.getType()).toArray(Class[]::new);
        // check if method adheres to the signature
        return ClassUtils.isAssignable(paramClasses, getSignature());
    }
    default int arity() {
        return getSignature().length;
    }
    boolean canTarget(ParserTestCase testCase);
    Arguments getArgs(ParserTestCase testCase);

    public sealed interface ValidTestType extends AParserTestType {
        @Override
        default boolean canTarget(ParserTestCase testCase) {
            return testCase instanceof ParserTestCase.ValidTestCase<?>;
        }
        Arguments getArgs(ParserTestCase.ValidTestCase<?> testCase);
        default Arguments getArgs(ParserTestCase testCase) {
            return switch (testCase) {
                case ParserTestCase.ValidTestCase<?> v -> getArgs(v);
                default -> null;
            };
        }
    }
    public sealed interface ExceptionTestType extends AParserTestType {
        // todo check isParamMatch[1] is of CLass<? extends Throwable>
        default Arguments getArgs(ParserTestCase testCase) {
            return switch (testCase) {
                case ParserTestCase.ExceptionTestCase<?> e -> getArgs(e);
                default -> null;
            };
        }
        Arguments getArgs(ParserTestCase.ExceptionTestCase<?> testCase);
        @Override
        default boolean canTarget(ParserTestCase testCase) {
            return testCase instanceof ParserTestCase.ExceptionTestCase<?>;
        }
    }
    /*
     * signatures for valid test cases:
     * String input, T expected
     * String input, int expected
     * String input, T expected, int expectedMatch
     */
    public record ParseTestType() implements ValidTestType {
        private static Class<?>[] SIGNATURE = {String.class, Object.class};
        @Override public Class<?>[] getSignature() { return SIGNATURE; }

        @Override public boolean canTarget(ParserTestCase testCase) {
            return ValidTestType.super.canTarget(testCase) && ((ParserTestCase.ValidTestCase<?>) testCase).expectedParse() != null;
        }
        @Override public Arguments getArgs(ParserTestCase.ValidTestCase<?> testCase) {
            return Arguments.of(testCase.input(), testCase.expectedParse());
        }
    }
    public record MatchTestType() implements ValidTestType {
        private static Class<?>[] SIGNATURE = {String.class, int.class};
        @Override public Class<?>[] getSignature() { return SIGNATURE; }
        @Override public Arguments getArgs(ParserTestCase.ValidTestCase<?> testCase) {
            return Arguments.of(testCase.input(), testCase.expectedMatch());
        }
    }
    public record ParseMatchTestType() implements ValidTestType {
        private static Class<?>[] SIGNATURE = {String.class, Object.class, int.class};
        @Override public Class<?>[] getSignature() { return SIGNATURE; }
        @Override public Arguments getArgs(ParserTestCase.ValidTestCase<?> testCase) {
            return Arguments.of(testCase.input(), testCase.expectedParse(), testCase.expectedMatch());
        }
    }

    /*
     * signatures for exception test cases:
     * String input, Class<? extends Throwable> expectedThrown
     * String input, Class<? extends Throwable> expectedThrown, String thrownErrorMessage
     * String input, Class<? extends Throwable> expectedThrown, T additionalExceptionInfo
     * String input, Class<? extends Throwable> expectedThrown, String thrownErrorMessage, T additionalExceptionInfo
     */
    public record SimpleExceptionTestType() implements ExceptionTestType {
        private static Class<?>[] SIGNATURE = {String.class, Class.class};
        @Override public Class<?>[] getSignature() { return SIGNATURE; }
        @Override public Arguments getArgs(ParserTestCase.ExceptionTestCase<?> testCase) {
            return Arguments.of(testCase.input(), testCase.throwable());
        }
    }
    public record MessageExceptionTestCase() implements ExceptionTestType {
        private static Class<?>[] SIGNATURE = {String.class, Class.class, String.class};
        @Override public Class<?>[] getSignature() { return SIGNATURE; }
        @Override public Arguments getArgs(ParserTestCase.ExceptionTestCase<?> testCase) {
            return Arguments.of(testCase.input(), testCase.throwable(), testCase.errorMessage());
        }
    }
    public record InfoExceptionTestType() implements ExceptionTestType {
        private static Class<?>[] SIGNATURE = {String.class, Class.class, Object.class};
        @Override public Class<?>[] getSignature() { return SIGNATURE; }
        @Override public Arguments getArgs(ParserTestCase.ExceptionTestCase<?> testCase) {
            return Arguments.of(testCase.input(), testCase.throwable(), testCase.additionalInfo());
        }
    }
    public record MessageInfoExceptionTest() implements ExceptionTestType {
        private static Class<?>[] SIGNATURE = {String.class, Class.class, String.class, Object.class};
        @Override public Class<?>[] getSignature() { return SIGNATURE; }
        @Override public Arguments getArgs(ParserTestCase.ExceptionTestCase<?> testCase) {
            return Arguments.of(testCase.input(), testCase.throwable(), testCase.errorMessage(), testCase.additionalInfo());
        }
    }
    public static final List<AParserTestType> TEST_TYPES = List.of(
        // arity 2
        new MatchTestType(),           // {String.class, int.class}
        new SimpleExceptionTestType(), // {String.class, Class.class};
        new ParseTestType(),           // {String.class, Object.class}
        // arity 3
        new ParseMatchTestType(),       // {String.class, Object.class, int.class}
        new MessageExceptionTestCase(), // {String.class, Class.class, String.class}
        new InfoExceptionTestType(),    // {String.class, Class.class, Object.class}
        // arity 4
        new MessageInfoExceptionTest()  // {String.class, Class.class, String.class, Object.class}
    );
    public static AParserTestType getTestType(Method method) {
        Parameter[] parameters = method.getParameters();
        for(AParserTestType type : TEST_TYPES) {
            if(type.isParamMatch(parameters)) {
                return type;
            }
        }
        return null;
    }
}

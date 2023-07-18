package io.hostilerobot.ceramicrelief.controller.parser;

import org.apache.commons.lang3.ClassUtils;

import java.lang.reflect.Parameter;
import java.util.Arrays;

public sealed interface ParserTestCase {
    record ValidTestCase<T>(String input, T expectedParse, int expectedMatch) implements ParserTestCase {}
    record ExceptionTestCase<E>(String input, Class<? extends Throwable> throwable, String errorMessage, E additionalInfo) implements ParserTestCase {}
}

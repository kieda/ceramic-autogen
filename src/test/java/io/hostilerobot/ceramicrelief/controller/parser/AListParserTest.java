package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.ast.ADecimal;
import io.hostilerobot.ceramicrelief.controller.ast.AList;
import io.hostilerobot.ceramicrelief.controller.ast.ANode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AListParserTest {
    private static ADecimalParser DECIMALS = new ADecimalParser();

    // (1, 2, (1, 2), 3)
    private static sealed interface DoubleNode{}
    private static final record DoubleList(DoubleNode... items) implements DoubleNode{
        public int size() {
            return items().length;
        }

        @Override
        public String toString() {
            return Arrays.stream(items).map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        }
    }
    private static final record DoubleLeaf(double item) implements DoubleNode{
        @Override
        public String toString() {
            return String.valueOf(item);
        }
    }
    private static DoubleList dlist(double... items) {
        DoubleLeaf[] nodes = new DoubleLeaf[items.length];
        for (int i = 0; i < items.length; i++) {
            nodes[i] = leaf(items[i]);
        }
        return nlist(nodes);
    }
    private static DoubleList nlist(DoubleNode... items) {
        return new DoubleList(items);
    }
    private static DoubleLeaf leaf(double item) {
        return new DoubleLeaf(item);
    }

    public static class SimpleDoubleListArguments extends ParserTestArguments<DoubleList>{
        {
            addStrippedTest("()", nlist());
            String test0 = """
                    (
                        # empty list!
                    )""";
            addTest(test0 + " #comment afterwards", nlist(), test0.length());
            addStrippedTest("(1)", dlist(1));
            addStrippedTest("(-12.345)", dlist(-12.345));
            addStrippedTest("(1, -40)", dlist(1, -40));
            addStrippedTest("""
                    (  -40,  1.0 #comment
                       , 123 ) 
                    """, dlist(-40, 1, 123));
            String test1 = "(1,2,3,4,5,  6,+.123)";
            addTest(test1 + ") #haha my braces are unmatched!", // we will only match up to the end of the list.
                    dlist(1,2,3,4,5,6,.123), test1.length());
        }
    }

    public static class DoubleListArguments extends ParserTestArguments<DoubleList>{
        {
            addStrippedTest("((()))", nlist(nlist(nlist())));
            addStrippedTest("((()), #comment here!\n ((#and here for some reason\n), ((),()), (()), ())) ",
                    nlist(nlist(nlist()), nlist(nlist(), nlist(nlist(),nlist()), nlist(nlist()), nlist()))
            );
            addStrippedTest("((((((( -  123.456))\t))))) ", nlist(nlist(nlist(nlist(nlist(nlist(dlist(-123.456))))))));
            addStrippedTest("(((1, 2,3),4),5, #comment here!\n ((#and here for some reason\n6), ((7),(8),9), 10, ((11)), (12,13)), 14,15) ",
                    nlist(nlist(dlist(1,2,3),leaf(4)),leaf(5),
                            nlist(dlist(6), nlist(dlist(7),dlist(8), leaf(9)), leaf(10), nlist(dlist(11)), dlist(12, 13)), leaf(14), leaf(15))
            );
        }
    }

    // todo add more tests on exceptional cases
    // todo add more tests on mixed types - e.g. pairs and sections
    // todo add more tests with primitive types - e.g. doubles and names
    // todo better exceptions - find codepoints where an error occurs and be able to pass this to the user.

    public static void assertDoubleListEquals(DoubleList expectedList, AList<?> actualList) {
        assertEquals(expectedList.size(), actualList.size(), "%s.size() != %s.size()".formatted(expectedList, actualList));
        ANode<?>[] actualItems = actualList.getValue();
        for(int i = 0; i < expectedList.size(); i++) {
            ANode<?> actualNode = actualItems[i];
            switch(expectedList.items[i]) {
                case DoubleLeaf(double expectedDouble) -> {
                    assertEquals(ADecimal.class, actualNode.getClass());
                    ADecimal decimalNode = (ADecimal) actualNode;
                    assertEquals(expectedDouble, decimalNode.getValue());
                }
                case DoubleList list -> {
                    assertEquals(AList.class, actualNode.getClass());
                    AList<?> listNode = (AList<?>) actualNode;
                    assertDoubleListEquals(list, listNode); // recurse
                }
            }
        }
    }

    private static final AListParser<Double> DOUBLE_LIST = new AListParser<>(List.of(DECIMALS));
    @ParameterizedTest
    @ArgumentsSource(SimpleDoubleListArguments.class)
    public void testSimpleValidDoubles(String input, DoubleList expected, int expectedMatchLen) {
        int actualMatchLength = DOUBLE_LIST.match(input);
        assertEquals(expectedMatchLen, actualMatchLength);
        AList<Double> actual = DOUBLE_LIST.parse(input);
        assertDoubleListEquals(expected, actual);
    }
    private static final AListParser<?> DOUBLE_AND_LIST;
    static {
        final List<AParser<?>> DOUBLE_AND_LIST_PARSERS = new ArrayList<>();
        DOUBLE_AND_LIST_PARSERS.add(null); // used to parse lists
        DOUBLE_AND_LIST_PARSERS.add(DECIMALS);
        DOUBLE_AND_LIST = new AListParser<>(DOUBLE_AND_LIST_PARSERS);
        DOUBLE_AND_LIST_PARSERS.set(0, DOUBLE_AND_LIST);
    }

    @ParameterizedTest
    @ArgumentsSource(SimpleDoubleListArguments.class)
    @ArgumentsSource(DoubleListArguments.class)
    public void testComplexValidDoubles(String input, DoubleList expected, int expectedMatchLen) {
        int actualMatchLength = DOUBLE_AND_LIST.match(input);
        assertEquals(expectedMatchLen, actualMatchLength);
        AList<?> actual = DOUBLE_AND_LIST.parse(input);
        assertDoubleListEquals(expected, actual);
    }
}
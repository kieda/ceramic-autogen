package io.hostilerobot.ceramicrelief.controller.parser;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AListParserTest {
    private static ADecimalParser DECIMALS = new ADecimalParser();
    private static AListParser<Double> DOUBLE_LIST = new AListParser<>(List.of(DECIMALS));

    @Test
    public void test() {
        var v = DOUBLE_LIST.parse("(     #comment a((df\n" +
                ")");
        var w = DOUBLE_LIST.parse("(1)");
        var x = DOUBLE_LIST.parse("(1,-40)");
        var x2 = DOUBLE_LIST.parse("(  1,  -40 , #comment\n" +
                "  123   )");
        System.out.println(v);
        System.out.println(w);
        System.out.println(x);
        System.out.println(x2);
    }
}
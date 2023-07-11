package io.hostilerobot.ceramicrelief.controller.parser;

import io.hostilerobot.ceramicrelief.controller.parser.advancer.AdvancerState;
import io.hostilerobot.ceramicrelief.controller.parser.advancer.CharAdvancer;
import io.hostilerobot.ceramicrelief.util.SealedEnum;

public sealed class SealedTest extends SealedEnum<SealedTest> implements CharAdvancer<AdvancerState> {
    public SealedTest() {
        super(SealedTest.class);

    }

    @Override
    public void accept(char c, AdvancerState state) {

    }

    @Override
    public boolean test(char c, AdvancerState state) {
        return false;
    }

    private final static class Class1 extends SealedTest{}
}

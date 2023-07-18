package io.hostilerobot.ceramicrelief.controller.parser.advancer;

public class ChainedAdvancerState<A extends AdvancerState, B extends AdvancerState> extends AdvancerState{
    private final A advancerA;
    private final B advancerB;
    public ChainedAdvancerState(A advancerA, B advancerB) {
        this.advancerA = advancerA;
        this.advancerB = advancerB;
    }

    @Override
    protected void encounterValueChar(char c) {
        advancerA.encounterValueChar(c);
        advancerB.encounterValueChar(c);
    }

    private void syncPos() {
        boolean increaseA;
        while((increaseA = (advancerA.getPos() < advancerB.getPos() && !advancerA.isStopped())) ||
                ((advancerB.getPos() < advancerA.getPos() && !advancerB.isStopped()))) {
            if(increaseA) {
                advancerA.increasePos();
            } else {
                advancerB.increasePos();
            }
        }

        int maxPos = Math.max(advancerA.getPos(), advancerB.getPos());
        while(!super.isStopped() && super.getPos() < maxPos) {
            super.increasePos();
        }
    }

    @Override
    protected void stop() {
        super.stop();
        advancerA.stop();
        advancerB.stop();
    }

    @Override
    protected void increasePos() {
        syncPos();
        super.increasePos();
        advancerA.increasePos();
        advancerB.increasePos();
    }

    @Override
    public boolean isStopped() {
        return advancerB.isStopped() || advancerA.isStopped() || super.isStopped();
    }

    @Override
    public boolean hasValue() {
        return advancerB.hasValue() || advancerA.hasValue();
    }

    @Override
    public int getPos() {
        syncPos();

        return Math.max(Math.max(advancerA.getPos(), advancerB.getPos()), super.getPos());
    }

    public A getA() {
        return advancerA;
    }
    public B getB() {
        return advancerB;
    }

    public static <A extends AdvancerState, B extends AdvancerState> ChainedAdvancerState<A, B>
            chain(A advancerA, B advancerB) {
        return new ChainedAdvancerState<>(advancerA, advancerB);
    }
    public static <A extends AdvancerState, B extends AdvancerState, C extends AdvancerState>
            ChainedAdvancerState<A, ChainedAdvancerState<B, C>> chain(A advancerA, B advancerB, C advancerC) {
        return new ChainedAdvancerState<>(advancerA, new ChainedAdvancerState<>(advancerB, advancerC));
    }
}

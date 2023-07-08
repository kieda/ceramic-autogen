package io.hostilerobot.ceramicrelief.controller.ast;

public class ADecimal implements ANode<Double> {
    private final double val;
    public ADecimal(double val) {
        this.val = val;
    }

    @Override
    public Double getValue() {
        return val;
    }
    @Override
    public int size() {
        return 1;
    }
}

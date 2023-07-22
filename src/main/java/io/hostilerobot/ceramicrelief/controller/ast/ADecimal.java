package io.hostilerobot.ceramicrelief.controller.ast;

import java.util.Objects;

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

    @Override
    public String toString() {
        return String.valueOf(val);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ADecimal aDecimal = (ADecimal) o;
        return Double.compare(aDecimal.val, val) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(val);
    }
}

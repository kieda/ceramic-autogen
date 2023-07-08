package io.hostilerobot.ceramicrelief.controller.ast;

import org.apache.commons.math.fraction.Fraction;

public class AQuotient implements ANode<Fraction> {
    private final Fraction fraction;

    public AQuotient(int numerator, int denominator) {
        this(new Fraction(numerator, denominator));
    }

    public AQuotient(Fraction fraction){
        this.fraction = fraction;
    }

    @Override
    public Fraction getValue() {
        return fraction;
    }
    @Override
    public int size() {
        return 1;
    }
}

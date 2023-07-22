package io.hostilerobot.ceramicrelief.controller.ast;

import org.apache.commons.math.fraction.Fraction;

import java.util.Objects;

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

    @Override
    public String toString() {
        return String.valueOf(fraction);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AQuotient aQuotient = (AQuotient) o;
        return Objects.equals(fraction, aQuotient.fraction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(fraction);
    }
}

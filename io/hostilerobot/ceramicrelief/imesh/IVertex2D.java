package io.hostilerobot.ceramicrelief.imesh;

import org.apache.commons.math.fraction.Fraction;

import java.util.Objects;

public class IVertex2D {
    // use fractions for extra spicy exactness and also so we don't have to deal with strange floating point BS
    private Fraction x;
    private Fraction y;

    public IVertex2D(Fraction x, Fraction y) {
        this.x = x;
        this.y = y;
    }

    public Fraction getX() {
        return x;
    }

    public Fraction getY() {
        return y;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IVertex2D vertex2D = (IVertex2D) o;
        return Objects.equals(x, vertex2D.x) && Objects.equals(y, vertex2D.y);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }
}

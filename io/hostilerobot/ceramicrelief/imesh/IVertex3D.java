package io.hostilerobot.ceramicrelief.imesh;

import org.apache.commons.math.fraction.Fraction;

import java.util.Objects;

/**
 * intermediate vertex representation. We use quotients rather than floats
 */
public class IVertex3D {
    // use fractions for extra spicy exactness and also so we don't have to deal with strange floating point BS
    private Fraction x;
    private Fraction y;
    private Fraction z;

    public IVertex3D(Fraction x, Fraction y, Fraction z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Fraction getX() {
        return x;
    }

    public Fraction getY() {
        return y;
    }

    public Fraction getZ() {
        return z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IVertex3D vertex3D = (IVertex3D) o;
        return Objects.equals(x, vertex3D.x) && Objects.equals(y, vertex3D.y) && Objects.equals(z, vertex3D.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }
}

package io.hostilerobot.ceramicrelief.qmesh;

import org.apache.commons.math.fraction.Fraction;

import java.util.Objects;

/**
 * intermediate vertex representation. We use quotients rather than floats
 */
public class QVertex3D {
    // use fractions for extra spicy exactness and also so we don't have to deal with strange floating point BS
    private Fraction x;
    private Fraction y;
    private Fraction z;

    public QVertex3D() {
        this.x = Fraction.ZERO;
        this.y = Fraction.ZERO;
        this.z = Fraction.ZERO;
    }

    public QVertex3D(QVertex3D other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }
    public QVertex3D(Fraction x, Fraction y, Fraction z) {
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

    public void setX(Fraction x) {
        this.x = x;
    }

    public void setY(Fraction y) {
        this.y = y;
    }

    public void setZ(Fraction z) {
        this.z = z;
    }
    public void set(Fraction x, Fraction y, Fraction z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QVertex3D vertex3D = (QVertex3D) o;
        return Objects.equals(x, vertex3D.x) && Objects.equals(y, vertex3D.y) && Objects.equals(z, vertex3D.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public Fraction dot(QVertex3D other) {
        return this.x.multiply(other.x)
                .add(this.y.multiply(other.y))
                .add(this.z.multiply(other.z));
    }

    public QVertex3D cross(QVertex3D other) {
        QVertex3D cross = new QVertex3D();
        cross(this, other, cross);
        return cross;
    }

    public static void cross(QVertex3D v1, QVertex3D v2, QVertex3D dest) {
        if(v1.equals(v2)) {
            dest.x = Fraction.ZERO;
            dest.y = Fraction.ZERO;
            dest.z = Fraction.ZERO;
            return;
        }
        // x = v1.y*v2.z - v1.z*v2.y
        // y = v1.z*v2.x - v1.x*v2.z
        // z = v1.x*v2.y - v1.y*v2.x
        Fraction xd = (v1.y.multiply(v2.z)).subtract(v1.z.multiply(v2.y)); // temp vars in case v1 or v2 == dest
        Fraction yd = (v1.z.multiply(v2.x)).subtract(v1.x.multiply(v2.z));
        dest.z = (v1.x.multiply(v2.y)).subtract(v1.y.multiply(v2.x));
        dest.y = yd;
        dest.x = xd;
    }

    public QVertex3D subtract(QVertex3D other) {
        QVertex3D sub = new QVertex3D();
        subtract(this, other, sub);
        return sub;
    }

    public static void subtract(QVertex3D v1, QVertex3D v2, QVertex3D dest) {
        if(v1.equals(v2)) {
            dest.x = Fraction.ZERO;
            dest.y = Fraction.ZERO;
            dest.z = Fraction.ZERO;
            return;
        }
        dest.x = v1.x.subtract(v2.x);
        dest.y = v1.y.subtract(v2.y);
        dest.z = v1.z.subtract(v2.z);
    }

    public QVertex3D add(QVertex3D other) {
        QVertex3D add = new QVertex3D();
        add(this, other, add);
        return add;
    }

    public static void add(QVertex3D v1, QVertex3D v2, QVertex3D dest) {
        dest.x = v1.x.add(v2.x);
        dest.y = v1.y.add(v2.y);
        dest.z = v1.z.add(v2.z);
    }

    public double length() {
        return Math.sqrt(this.dot(this).doubleValue());
    }
}

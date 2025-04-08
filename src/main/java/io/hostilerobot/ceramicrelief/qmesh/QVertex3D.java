package io.hostilerobot.ceramicrelief.qmesh;

import io.hostilerobot.ceramicrelief.util.Epsilon;
import org.apache.commons.math.fraction.Fraction;

import java.util.Objects;

/**
 * intermediate vertex representation. We use quotients rather than floats
 */
public class QVertex3D {
    private double x;
    private double y;
    private double z;

    public QVertex3D() {
        this.x = 0;
        this.y = 0;
        this.z = 0;
    }

    public QVertex3D(QVertex3D other) {
        this.x = other.x;
        this.y = other.y;
        this.z = other.z;
    }
    public QVertex3D(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double getX() {
        return x;
    }
    public double getY() {
        return y;
    }
    public double getZ() {
        return z;
    }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public void setZ(double z) {
        this.z = z;
    }
    public void set(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QVertex3D vertex3D = (QVertex3D) o;
        return Epsilon.equals(x, vertex3D.x) && Epsilon.equals(y, vertex3D.y) && Epsilon.equals(z, vertex3D.z);
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y, z);
    }

    public double dot(QVertex3D other) {
        return this.x * other.x
                + this.y * other.y
                + this.z * other.z;
    }

    public QVertex3D cross(QVertex3D other) {
        QVertex3D cross = new QVertex3D();
        cross(this, other, cross);
        return cross;
    }

    public static void cross(QVertex3D v1, QVertex3D v2, QVertex3D dest) {
        if(v1.equals(v2)) {
            dest.x = 0;
            dest.y = 0;
            dest.z = 0;
            return;
        }
        // x = v1.y*v2.z - v1.z*v2.y
        // y = v1.z*v2.x - v1.x*v2.z
        // z = v1.x*v2.y - v1.y*v2.x
        double xd = (v1.y * v2.z) - (v1.z * v2.y); // temp vars in case v1 or v2 == dest
        double yd = (v1.z * v2.x) - (v1.x * v2.z);
        dest.z = (v1.x * v2.y) - (v1.y * v2.x);
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
            dest.x = 0;
            dest.y = 0;
            dest.z = 0;
            return;
        }
        dest.x = v1.x - v2.x;
        dest.y = v1.y - v2.y;
        dest.z = v1.z - v2.z;
    }

    public QVertex3D add(QVertex3D other) {
        QVertex3D add = new QVertex3D();
        add(this, other, add);
        return add;
    }

    public static void add(QVertex3D v1, QVertex3D v2, QVertex3D dest) {
        dest.x = v1.x + v2.x;
        dest.y = v1.y + v2.y;
        dest.z = v1.z + v2.z;
    }

    public double length() {
        return Math.sqrt(this.dot(this));
    }
}

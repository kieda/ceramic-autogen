package io.hostilerobot.ceramicrelief.texture.mesh_traversal.intersection;

import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.github.davidmoten.rtree2.geometry.internal.RectangleDouble;
import io.hostilerobot.ceramicrelief.util.Epsilon;
import javafx.geometry.Point2D;
import org.apache.commons.math.util.FastMath;

/**
 * take two on triangle intersection
 *
 * this time, we do something radically cool.
 *
 * First principle is "the dividing line". We know that two triangles do not intersect if and only if we can find a line
 * that divides the two triangles. Typically, this is done by looking at each line made by the triangle and its convex
 * outer area, and seeing if the cross product from that line to each of the three points are in the same direction.
 * This would need to be done in both directions
 *
 * Since this is for use in an RTree, we may precompute some values to speed up each test significantly.
 *
 * We precompute a matrix that normalizes this triangle to (0,0), (0,1), (1, 0)
 * This is some transformation matrix X.
 * Then we can apply the transformation X to a different triangle to put it in that basis.
 * Then, we simply need to check that all points of the new triangle have x < 0, y < 0, or x + y > 1
 *
 *  A transformation has the form
 *
 *  [ x' ]     =  [ a b ]  [ x ]  + [ e ]
 *  [ y' ]        [ c d ]  [ y ]    [ f ]
 *
 *  Which we can describe as
 *  [ x' ]         [ a b e ]    [ x ]
 *  [ y' ]     =   [ c d f ]  * [ y ]
 *  [ 1  ]         [ 0 0 1 ]    [ 1 ]
 *
 *  We can build a system of equations for our three point
 *  [ 0  1  0 ]     [ a b e ]    [ x_1 x_2 x_3 ]
 *  [ 0  0  1 ]  =  [ c d f ] *  [ y_1 y_2 y_3 ]
 *  [ 1  1  1 ]     [ 0 0 1 ]    [  1   1   1  ]
 *
 *  Or A = X * T
 *
 *  We can invert to get
 *
 *  A * T^-1 = X * T * T^-1
 *  A * T^-1 = X
 *
 *  So we precompute X = A * T^-1, then when we're comparing to another triangle T2 we just multiple
 *  X * T2 = T2', and check the system of equations
 */
public class Triangle2D implements Rectangle {
    // bounding box
    private Rectangle bounds = null;

    // transformation matrix
    private double a, b, e,
                   c, d, f;
    //             0, 0, 1    (implicitly)

    // for testing if a point is in this triangle
    private double pointInTriU1x, pointInTriU1y, pointInTriU2,
        pointInTriV1x, pointInTriV1y, pointInTriV2;

    // points of this triangle
    private Point2D v1, v2, v3;

    private void computeMinRectangleBounds(Point2D v1, Point2D v2, Point2D v3) {
        double minX;
        double maxX;
        if(v1.getX() < v2.getX()) {
            minX = v1.getX();
            maxX = v2.getX();
        } else {
            minX = v2.getX();
            maxX = v1.getX();
        }
        minX = Math.min(minX, v3.getX());
        maxX = Math.max(maxX, v3.getX());

        double minY;
        double maxY;
        if(v1.getY() < v2.getY()) {
            minY = v1.getY();
            maxY = v2.getY();
        } else {
            minY = v2.getY();
            maxY = v1.getY();
        }
        minY = Math.min(minY, v3.getY());
        maxY = Math.max(maxY, v3.getY());

        bounds = RectangleDouble.create(minX, minY, maxX, maxY);
    }

    private void computePointInTest(Point2D a, Point2D b, Point2D c) {
        double v0x = a.getX() - c.getX();
        double v0y = a.getY() - c.getY();
        double v1x = b.getX() - c.getX();
        double v1y = b.getY() - c.getY();
        double v0v0 = v0x*v0x + v0y*v0y;
        double v1v1 = v1x*v1x + v1y*v1y;
        double v0v1 = v0x*v1x + v0y*v1y;
        // denom is only 0 if (v0 . v0)(v1 . v1) == (v0 . v1)^2
        // this would mean ||v0||^2 ||v1||^2 == (||v0|| ||v1|| cos(theta))^2
        // which would imply theta == 0, meaning there is no angle between the two vecors (no area triangle)
        double denom = v0v0*v1v1 - v0v1*v0v1;
        denom = denom < Epsilon.epsilon() ? 0.0 : denom; // if denom is effectively zero then we just let the values U1, U2, V1, V2 go infinite
        // this will effectively make all point-in-triangle tests return false on this triangle

        // calculating values for U
        double U1x = (v1v1*v0x - v0v1*v1x);
        double U1y = (v1v1*v0y - v0v1*v1y);
        double U2 = c.getX()*U1x + c.getY()*U1y;
        pointInTriU1x = U1x/denom;
        pointInTriU1y = U1y/denom;
        pointInTriU2 = U2/denom;

        // calculating values for V
        double V1x = v0v0*v1x - v0v1*v0x;
        double V1y = v0v0*v1y - v0v1*v0y;
        double V2 = c.getX()*V1x + c.getY()*V1y;
        pointInTriV1x = V1x/denom;
        pointInTriV1y = V1y/denom;
        pointInTriV2 = V2/denom;
        // v2 = u * v0 + v * v1
        // v0 = C - A
        // v1 = B - A
        // v2 = point - A
        // u = ((v1.v1)(v2.v0)-(v1.v0)(v2.v1)) / ((v0.v0)(v1.v1) - (v0.v1)(v1.v0))
        //      (v1x*v1x + v1y*v1y)(v2x*v0x + v2y*v0y) - (v1x*v0x + v1y*v0y)(v2x*v1x + v2y*v1y)
        //      v2.((v1.v1)(v0)-(v1.v0)(v1))
        //      v2.((v1x*v1x + v1y*v1y)(v0)-(v1x*v0x + v1y*v0y)(v1))
        //      v2x*((v1x*v1x + v1y*v1y)(v0x)-(v1x*v0x + v1y*v0y)(v1x)) + v2y*((v1x*v1x + v1y*v1y)(v0y)-(v1x*v0x + v1y*v0y)(v1y))
        //      v2x*U1x + v2y*U1y
        //      (pointx - Ax)*U1x + (pointy - Ay)*U1y
        //      pointx*U1x + pointy*U1y - Ax*U1x - Ay*U1y
        //      U2 = Ax*U1x + Ay*U1y
        // v = ((v0.v0)(v2.v1)-(v0.v1)(v2.v0)) / ((v0.v0)(v1.v1) - (v0.v1)(v1.v0))
        //     v2.((v0.v0)v1 - (v0.v1)v0)
        //     v2x((v0x*v0x + v0y*v0y)v1x - (v0x*v1x + v0y*v1y)v0x) + v2y((v0x*v0x + v0y*v0y)v1y - (v0x*v1x + v0y*v1y)v0y)
        //     (pointx - Ax)*V1x + (pointy - Ay)*V1y
        //     pointx*V1x + pointy*V1y - Ax*V1x - Ay*V1y
        //     V2 = Ax*V1x + Ay*V1y
    }

    private void computeNormalizationMatrix(Point2D a, Point2D b, Point2D c) {
        // denominator of the transformation matrix
        // (aX*bY - aX*cY - bX*aY + bX*cY + cX*aY - cX*bY)
        // this is zero if and only if these three are in a line
        double denom = (a.getX()*b.getY() - a.getX()*c.getY() - b.getX()*a.getY() + b.getX()*c.getY() + c.getX()*a.getY() - c.getX()*b.getY());
        // also, how do we transform something that is a line?
        if(Epsilon.isZero(denom)) {
            denom = 0.0;
        }

        /*
                    [ a | b | e ]
        transform = [ c | d | f ]
                    [ 0 | 0 | 1 ]

                              [ (cY - aY) | (aX - cX) | (cX aY - aX cY) ]
        transform = 1/denom * [ (aY - bY) | (bX - aX) | (aX bY - bX aY) ]
                              [     0     |     0     |        1        ]
         */

        this.a = (c.getY() - a.getY())/denom;
        this.b = (a.getX() - c.getX())/denom;
        this.c = (a.getY() - b.getY())/denom;
        this.d = (b.getX() - a.getX())/denom;
        this.e = (c.getX()*a.getY() - a.getX()*c.getY())/denom;
        this.f = (a.getX()*b.getY() - b.getX()*a.getY())/denom;
    }

    private void precompute(Point2D v1, Point2D v2, Point2D v3) {
        // compute min rectangle bounds
        computeMinRectangleBounds(v1, v2, v3);
        // compute values so testing if a point is in the triangle is a simple translation
        computePointInTest(v1, v2, v3);
        // compute a normalization matrix
        computeNormalizationMatrix(v1, v2, v3);

        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }


    public Triangle2D(Point2D v1, Point2D v2, Point2D v3) {
        precompute(v1, v2, v3);
    }

    private boolean inTriangle(double xPos, double yPos) {
        double u = pointInTriU1x*xPos + pointInTriU1y*yPos - pointInTriU2;
        double v = pointInTriV1x*xPos + pointInTriV1y*yPos - pointInTriV2;
        return Epsilon.betweenZeroOneExclusive(u) && Epsilon.betweenZeroOneExclusive(v) && Epsilon.betweenZeroOneExclusive(u + v);
    }


    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    // this warning is a bit silly, since inverting the method would not be right
    // we don't test for an intersection, rather we test for conditions where we know there won't be one.
    // if this were inverted, returning true would not imply the existence of an intersection!
    private static boolean hasNoIntersection(Triangle2D t1, Triangle2D t2) {
        // map triangle to our coordinates
        //         t2'                t1             t2
        //  [ x1'  x2'  x3' ]     [ a b e ]    [ x_1 x_2 x_3 ]
        //  [ y1'  y2'  y3' ]  =  [ c d f ] *  [ y_1 y_2 y_3 ]
        //  [  1    1    1  ]     [ 0 0 1 ]    [  1   1   1  ]

        double x1 = t1.a * t2.v1.getX() + t1.b * t2.v1.getY() + t1.e;
        double y1 = t1.c * t2.v1.getX() + t1.d * t2.v1.getY() + t1.f;
        double x2 = t1.a * t2.v2.getX() + t1.b * t2.v2.getY() + t1.e;
        double y2 = t1.c * t2.v2.getX() + t1.d * t2.v2.getY() + t1.f;
        double x3 = t1.a * t2.v3.getX() + t1.b * t2.v3.getY() + t1.e;
        double y3 = t1.c * t2.v3.getX() + t1.d * t2.v3.getY() + t1.f;

        return  // transformed triangle: all points below x axis, does not intersect
                (Epsilon.lessThanZeroInclusive(x1) && Epsilon.lessThanZeroInclusive(x2) && Epsilon.lessThanZeroInclusive(x3))
                // transformed triangle: all points below y axis, does not intersect
                || (Epsilon.lessThanZeroInclusive(y1) && Epsilon.lessThanZeroInclusive(y2) && Epsilon.lessThanZeroInclusive(y3))
                // transformed triangle: all points above y = 1 - x line, does not intersect
                || (Epsilon.greaterThanOneInclusive(x1 + y1) && Epsilon.greaterThanOneInclusive(x2 + y2) && Epsilon.greaterThanOneInclusive(x3 + y3));
    }

    private boolean testRectangle(Point2D a, Point2D b, Rectangle rectangle) {
        double posX = a.getX();
        double posY = a.getY();
        double lenX = b.getX() - a.getX();
        double lenY = b.getY() - a.getY();
        if(Epsilon.lessThanZeroInclusive(FastMath.abs(lenX))) {
            // prevent divide by zero errors if we have a vertical line
            if(rectangle.x1() < posX && posX < rectangle.x2() && // vertical line at x is in x range of rectangle
                    overlapRange(posY, lenY, rectangle.y1(), rectangle.y2())) { // if the y values are also in range
                return true;
            }
        } else {
            // otherwise, we find the y values that this line will intersect at
            double m = lenY/lenX;
            double y0 = posY + m * (rectangle.x1() - posX);
            double y1 = posY + m * (rectangle.x2() - posX);

            return overlapRange(y0, y1 - y0, rectangle.y1(), rectangle.y2());
        }
        return false;
    }

    private static boolean overlapRange(double start, double direction, double rangeMin, double rangeMax) {
        // checks if (start, start + direction) is within the range of (rangeMin, rangeMax)
        if(direction > 0) {
            // no overlap: start > rangeMax || start + direction < rangeMin
            return rangeMax >= start && start + direction >= rangeMin;
        } else {
            return rangeMax + direction >= start && start >=rangeMin;
        }
    }

    @Override
    public boolean intersects(Rectangle rectangle) {
        if (rectangle instanceof Triangle2D triangle) {
            return !hasNoIntersection(this, triangle) &&
                    !hasNoIntersection(triangle, this);
        } else {
            // otherwise, use the following method: https://seblee.me/2009/05/super-fast-trianglerectangle-intersection-test/
            // lines are {POS_SOURCE_X, POS_SOURCE_Y} + {VAL_LEN_X, VAL_LEN_Y} * u
            return testRectangle(v1, v3, rectangle) ||
                    testRectangle(v1, v2, rectangle) ||
                    testRectangle(v2, v3, rectangle) ||
                    // final test:
                    // point from the rectangle. check if it's in bounds of this triangle. If it is,
                    // then the rectangle is entirely contained within this triangle
                    inTriangle(rectangle.x1(), rectangle.y1());
        }
    }

    @Override
    public double distance(Rectangle rectangle) {
        // note - I don't think we'll use this implementation for our searching
        // so we don't need to be fixated on optimizing it for speed
        if(intersects(rectangle)) return 0;
        throw new UnsupportedOperationException("distance");
    }

    @Override
    public Rectangle mbr() {
        return bounds;
    }

    @Override
    public double x1() {
        return bounds.x1();
    }

    @Override
    public double y1() {
        return bounds.y1();
    }

    @Override
    public double x2() {
        return bounds.x2();
    }

    @Override
    public double y2() {
        return bounds.y2();
    }

    @Override
    public double area() {
        // todo we can use this https://www.cuemath.com/measurement/area-of-triangle-with-3-sides/
        throw new UnsupportedOperationException();
    }

    @Override
    public double intersectionArea(Rectangle r) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double perimeter() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Rectangle add(Rectangle r) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean contains(double x, double y) {
        return inTriangle(x, y);
    }

    @Override
    public boolean isDoublePrecision() {
        return true;
    }

    @Override
    public Geometry geometry() {
        return this;
    }
}

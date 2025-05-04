package io.hostilerobot.ceramicrelief.texture.mesh_traversal.intersection;

import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.github.davidmoten.rtree2.geometry.internal.RectangleDouble;
import javafx.geometry.Point2D;

import java.awt.geom.Line2D;


/**
 * stupid, stable, slow triangle triangle intersection
 */
public class Triangle2DX implements Rectangle {

    private final Point2D a, b, c;

    private final Rectangle bounds;

    public Triangle2DX(Point2D a, Point2D b, Point2D c) {
        this.a = a;
        this.b = b;
        this.c = c;

        double maxX = Math.max(Math.max(a.getX(), b.getX()), c.getX());
        double minX = Math.min(Math.min(a.getX(), b.getX()), c.getX());
        double maxY = Math.max(Math.max(a.getY(), b.getY()), c.getY());
        double minY = Math.min(Math.min(a.getY(), b.getY()), c.getY());
        bounds = RectangleDouble.create(minX, minY, maxX, maxY);
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
    public boolean contains(double x, double y) {
        double xAx = x - a.getX();
        double CxAx = c.getX() - a.getX();
        double BxAx = b.getX() - a.getX();

        double ByAy = b.getY() - a.getY();

        double u = (ByAy * xAx - BxAx * (y - a.getY())) /
                (CxAx * ByAy - (c.getY() - a.getY()) * BxAx);
        double v = (xAx - u * CxAx) / BxAx;

        return u >= 0 && u <= 1 && v >= 0 && v <= 1
                && u + v >= 0 && u + v <= 1;
    }

    @Override
    public Rectangle mbr() {
        return bounds;
    }

    private Point2D[][] getLines() {
        return new Point2D[][]{
                {a, c},
                {a, b},
                {b, c}
        };
    }

    @Override
    public boolean intersects(Rectangle r) {
        if(r instanceof Triangle2DX triangle) {
            // A -> C vs R -> S
            // A -> B vs R -> S
            // B -> C vs R -> S

            // A -> C vs R -> T
            // A -> B vs R -> T
            // B -> C vs R -> T

            // A -> C vs S -> T
            // A -> B vs S -> T
            // B -> C vs S -> T

            for(Point2D[] lines : getLines()) {
                Point2D p1 = lines[0];
                Point2D p2 = lines[1];

                for(Point2D[] otherLines : triangle.getLines()) {
                    Point2D op1 = otherLines[0];
                    Point2D op2 = otherLines[1];
                    if(Line2D.linesIntersect(p1.getX(), p1.getY(), p2.getX(), p2.getY(),
                            op1.getX(), op1.getY(), op2.getX(), op2.getY())) {
                        return true;
                    }
                }
            }
            // final test - true if one of the points is inside one of the triangles, meaning all 3 are contained within
            return contains(triangle.a.getX(), triangle.a.getY())
                    || triangle.contains(a.getX(), a.getY());
        } else {
            for(Point2D[] lines : getLines()) {
                Point2D p1 = lines[0];
                Point2D p2 = lines[1];


                // test against rectangle bounds
                if(Line2D.linesIntersect(p1.getX(), p1.getY(), p2.getX(), p2.getY(), r.x1(), r.y1(), r.x2(), r.y1())
                    || Line2D.linesIntersect(p1.getX(), p1.getY(), p2.getX(), p2.getY(), r.x2(), r.y1(), r.x2(), r.y2())
                    || Line2D.linesIntersect(p1.getX(), p1.getY(), p2.getX(), p2.getY(), r.x1(), r.y1(), r.x1(), r.y2())
                    || Line2D.linesIntersect(p1.getX(), p1.getY(), p2.getX(), p2.getY(), r.x1(), r.y2(), r.x2(), r.y2())
                ) {
                    return true;
                }
            }

            // final test - just see if one is completely contained in the others
            return contains(r.x1(), r.y1())
                    || r.contains(a.getX(), a.getY());
        }
    }

    @Override
    public double distance(Rectangle r) {
        if(intersects(r)) return 0;
        throw new UnsupportedOperationException("distance");
    }

    @Override
    public double area() {
        throw new UnsupportedOperationException("area");
    }

    @Override
    public double intersectionArea(Rectangle r) {
        throw new UnsupportedOperationException("intersectionArea");
    }

    @Override
    public double perimeter() {
        throw new UnsupportedOperationException("perimeter");
    }

    @Override
    public Rectangle add(Rectangle r) {
        throw new UnsupportedOperationException("add");
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

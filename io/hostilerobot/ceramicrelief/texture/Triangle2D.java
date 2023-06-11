package io.hostilerobot.ceramicrelief.texture;

import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Line;
import com.github.davidmoten.rtree2.geometry.Point;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.github.davidmoten.rtree2.geometry.internal.RectangleDouble;
import javafx.geometry.Point2D;

import java.util.List;

// triangle implementation for bounding box
public class Triangle2D implements Geometry {
    private Point2D v1, v2, v3;
    private Point2D insertedPoint; // speeds up calculations, as we're traversing the mesh we're only adding one unique point

    private static final int INTERSECTION_LEN = 6;

    private static final int VAL_POINT_IN_TRI_U = 0;
    private static final int VAL_POINT_IN_TRI_V = 1;
    private static final int INTERSECTION_OFFSET = 2;
    private static final int VAL_INTERSECTION_H = 2;
    private static final int VAL_INTERSECTION_I = 3;
    private static final int VAL_INTERSECTION_J = 4;
    private static final int VAL_INTERSECTION_K = 5;

    private void precompute() {
        if(precomputedVals == null) {
            // do we calculate intersection for 2 edges or 3?
            int checkIntersectionCount = insertedPoint == null ? 3 : 2;
            precomputedVals = new double[INTERSECTION_OFFSET + INTERSECTION_LEN * checkIntersectionCount];
            Point2D vertexA, vertexB, vertexC;
            // vertexC is insertedPoint
            if(v1.equals(insertedPoint)) {
                vertexA = v2;
                vertexB = v3;
                vertexC = v1;
            } else if(v2.equals(insertedPoint)) {
                vertexA = v1;
                vertexB = v3;
                vertexC = v2;
            } else {
                vertexA = v1;
                vertexB = v2;
                vertexC = v3;
            }
            // for edge: C -> A
            {
                // decomposed such that intersection of two lines is at [u, v] where 0 <= u, v, u+v <= 1

                // where v = other.K[count] * (this.H[count] + other.J[count] * this.I[count)
                // and u =
                // NOTE: lines are parallel if (this.A.x - this.C.x) * (other.A.y - other.C.y) == (this.A.y - this.C.y) * (other.A.x - other.C.x)
                double I1 = vertexA.getY() - vertexC.getY();
                double H1 = I1 * (vertexC.getX() + vertexC.getY()) / (vertexA.getX() - vertexC.getX() - I1); // todo - what to do when denominator 0?
                double J1 = vertexA.getX() - vertexA.getY();
                double K1 = 1.0/(vertexC.getX() - vertexA.getX());

                // u = ((S_y - R_y) (C_x - R_x) - (S_x - R_x)(C_y-R_y)) / ((S_x - R_x)(A_y - C_y) - (S_y - R_y)(A_x - C_x))
                

            }
//            double K1 = 1.0/(vertex)

        }
    }

    public void freeUpSpace() {
        // after we're done using this object for intersectiontests free up space
        precomputedVals = null;
    }

    public boolean inTriangle(Point2D point) {

    }



    // precomputed values utilized in intersection testing
    private double[] precomputedVals = null;

    // since we keep the same list of points as a reference for our texture, we continue this pattern
    // by just passing in the indices that we utilize
    public Triangle2D(List<Point2D> vertices, int insertedPoint, int v1, int v2, int v3) {
        this(vertices.get(insertedPoint), vertices.get(v1), vertices.get(v2), vertices.get(v3));
    }
    public Triangle2D(Point2D insertedPoint, Point2D v1, Point2D v2, Point2D v3) {
        this.insertedPoint = insertedPoint;
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    @Override
    public double distance(Rectangle rectangle) {
        // note - I don't think we'll use this implementation for our searching
        // so we don't need to be fixated on optimizing it for speed
        if(intersects(rectangle)) return 0;
        throw new UnsupportedOperationException("distance");
//        if(rectangle instanceof Triangle2D) {
//
//        } else {
////            rectangle.
//        }
//        return 0;
    }

    private Rectangle bounds = null;

    @Override
    public Rectangle mbr() {
        if(bounds == null) {
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
        return bounds;
    }

    public Point2D getV1() {
        return v1;
    }

    public Point2D getV2() {
        return v2;
    }

    public Point2D getV3() {
        return v3;
    }

    @Override
    public boolean intersects(Rectangle rectangle) {
        if(rectangle instanceof Triangle2D) {
            Triangle2D triangle = (Triangle2D) rectangle;
            if(triangle.insertedPoint != null) {
                // one of the two edges added can insert. Let U, V represent barycentric coordinates for the two added edges
                // (xA, yA) + u (xU, yU) = (xP, yP) + s (xS, yS) + t (xT, yT)
                // xA - xP + u xU = s xS + t xT
                // yA - yP + u yU = s yS + t yT

                // A - B + u C = s D + t E
                // S - T + u U = s V + t W

                // idea: new added point is C. existing points are at A and B that coincide with a face.
                // triangle can be described as triangle = C + (A - C) * u + (B - C) * v
                // if there is an intersecion, there will be one where u = 0 or when v = 0
                // existing triangle RST can be triangle can be described as existing = R + (S - R) * s + (T - R) * t
                // or existing = S + (R - S) * r + (T - S) * t; or existing = T + (R - T) * r + (S - T) * s
                // if there is a triangle intersection, there will be one between the following:
                // from : C + (A - C) * u; 0 <= u <= 1
                //        C + (B - C) * v; 0 <= v <= 1
                // to   : R + (S - R) * s; 0 <= s <= 1
                //      : S + (T - S) * t; 0 <= t <= 1
                //      : T + (R - T) * r; 0 <= r <= 1
                // C + (A - C) * u = R + (S - R) * s'
            }

        }
        return false;
    }

    @Override
    public boolean isDoublePrecision() {
        return true;
    }
}

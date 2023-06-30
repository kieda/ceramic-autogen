package io.hostilerobot.ceramicrelief.texture;

import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.Rectangle;
import com.github.davidmoten.rtree2.geometry.internal.RectangleDouble;
import javafx.geometry.Point2D;
import org.apache.commons.math.util.FastMath;

import java.util.List;

// triangle implementation for bounding box intersection testing in RTree
// we only really hold precomputed values here useful for intersection testing.
// this class should not be used to find actual information about the triangle
// this class also errs on the side that two triangles are NOT intersecting, especially in the case where two triangles share an edge.
public class Triangle2D implements Rectangle {
    // bounding box
    private Rectangle bounds = null;
    // precomputed values utilized in intersection testing
    private double[] precomputedVals = null;

    // "close enough to zero" type of calculations
    // used for parallel lines, etc
    private final static double EPSILON = 0.0000001;

    // indices for precalculated values for testing if a point is in a triangle
    private static final int VAL_POINT_IN_TRI_U1_X = 0;
    private static final int VAL_POINT_IN_TRI_U1_Y = 1;
    private static final int VAL_POINT_IN_TRI_U2 = 2;
    private static final int VAL_POINT_IN_TRI_V1_X = 3;
    private static final int VAL_POINT_IN_TRI_V1_Y = 4;
    private static final int VAL_POINT_IN_TRI_V2 = 5;

    // indices for precalculated values for testing if edges of triangles intersect
    private static final int LENGTHS_COUNT = 4;
    private static final int LENGTHS_OFFSET = 6;
    // used to calculate intersecions
    private static final int VAL_POS_SOURCE_X = 0;
    private static final int VAL_POS_SOURCE_Y = 1;
    private static final int VAL_LEN_X = 2;
    private static final int VAL_LEN_Y = 3;

    private void makeMinRectangleBounds(Point2D v1, Point2D v2, Point2D v3) {
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


    private void precompute(Point2D insertedPoint, Point2D v1, Point2D v2, Point2D v3) {
        // compute min rectangle bounds
        makeMinRectangleBounds(v1, v2, v3);

        // do we calculate intersection for 2 edges or 3?
        int checkIntersectionCount = insertedPoint == null ? 3 : 2;
        precomputedVals = new double[LENGTHS_OFFSET + LENGTHS_COUNT * checkIntersectionCount];
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
            // also the case when insertedPoint is null.
            vertexA = v1;
            vertexB = v2;
            vertexC = v3;
        }

        {
            double v0x = vertexA.getX() - vertexC.getX();
            double v0y = vertexA.getY() - vertexC.getY();
            double v1x = vertexB.getX() - vertexC.getX();
            double v1y = vertexB.getY() - vertexC.getY();
            double v0v0 = v0x*v0x + v0y*v0y;
            double v1v1 = v1x*v1x + v1y*v1y;
            double v0v1 = v0x*v1x + v0y*v1y;
            // denom is only 0 if (v0 . v0)(v1 . v1) == (v0 . v1)^2
            // this would mean ||v0||^2 ||v1||^2 == (||v0|| ||v1|| cos(theta))^2
            // which would imply theta == 0, meaning there is no angle between the two vecors (no area triangle)
            double denom = v0v0*v1v1 - v0v1*v0v1;
            denom = denom < EPSILON ? 0.0 : denom; // if denom is effectively zero then we just let the values U1, U2, V1, V2 go infinite
                                                   // this will effectively make all point-in-triangle tests return false on this triangle

            // calculating values for U
            double U1x = (v1v1*v0x - v0v1*v1x);
            double U1y = (v1v1*v0y - v0v1*v1y);
            double U2 = vertexC.getX()*U1x + vertexC.getY()*U1y;
            precomputedVals[VAL_POINT_IN_TRI_U1_X] = U1x/denom;
            precomputedVals[VAL_POINT_IN_TRI_U1_Y] = U1y/denom;
            precomputedVals[VAL_POINT_IN_TRI_U2] = U2/denom;

            // calculating values for V
            double V1x = v0v0*v1x - v0v1*v0x;
            double V1y = v0v0*v1y - v0v1*v0y;
            double V2 = vertexC.getX()*V1x + vertexC.getY()*V1y;
            precomputedVals[VAL_POINT_IN_TRI_V1_X] = V1x/denom;
            precomputedVals[VAL_POINT_IN_TRI_V1_Y] = V1y/denom;
            precomputedVals[VAL_POINT_IN_TRI_V2] = V2/denom;
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

        precomputedVals[LENGTHS_OFFSET + VAL_POS_SOURCE_X] = vertexA.getX();
        precomputedVals[LENGTHS_OFFSET + VAL_POS_SOURCE_Y] = vertexA.getY();
        precomputedVals[LENGTHS_OFFSET + VAL_LEN_X] = vertexC.getX() - vertexA.getX();
        precomputedVals[LENGTHS_OFFSET + VAL_LEN_Y] = vertexC.getY() - vertexA.getY();
        precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_POS_SOURCE_X] = vertexC.getX();
        precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_POS_SOURCE_Y] = vertexC.getY();
        precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_LEN_X] = vertexB.getX() - vertexC.getX();
        precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_LEN_Y] = vertexB.getY() - vertexC.getY();
        // NOTE: lines are parallel if (this.A.x - this.C.x) * (other.A.y - other.C.y) == (this.A.y - this.C.y) * (other.A.x - other.C.x)
        // u = ((S_y - R_y) (C_x - R_x) - (S_x - R_x)(C_y - R_y)) / ((S_x - R_x)(A_y - C_y) - (S_y - R_y)(A_x - C_x))
        // v = ((A_y - C_y) (C_x - R_x) - (A_x - C_x)(C_y - R_y)) / ((S_x - R_x)(A_y - C_y) - (S_y - R_y)(A_x - C_x))
        // D = ((S_x - R_x)(A_y - C_y) - (S_y - R_y)(A_x - C_x))
        if(insertedPoint == null) {
            precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT*2 + VAL_POS_SOURCE_X] = vertexB.getX();
            precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT*2 + VAL_POS_SOURCE_Y] = vertexB.getY();
            precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT*2 + VAL_LEN_X] = vertexA.getX() - vertexB.getX();
            precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT*2 + VAL_LEN_Y] = vertexA.getY() - vertexB.getY();
        }
    }

    public void close() {
        // after we're done using this object for intersection tests free up space
        // we shouldn't use this object for more intersection tests after we're done, and we will throw an intersection
        // if we attempt to do more.
        precomputedVals = null;
        bounds = null;
    }

    public boolean inTriangle(double xPos, double yPos) {
        double u = precomputedVals[VAL_POINT_IN_TRI_U1_X]*xPos + precomputedVals[VAL_POINT_IN_TRI_U1_Y]*yPos - precomputedVals[VAL_POINT_IN_TRI_U2];
        double v = precomputedVals[VAL_POINT_IN_TRI_V1_X]*xPos + precomputedVals[VAL_POINT_IN_TRI_V1_Y]*yPos - precomputedVals[VAL_POINT_IN_TRI_V2];
        return inRangeExclusive(u) && inRangeExclusive(v) && inRangeExclusive(u + v);
    }

    public  Triangle2D(Point2D insertedPoint, Point2D v1, Point2D v2, Point2D v3) {
        precompute(insertedPoint, v1, v2, v3);
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

    private static boolean inRangeExclusive(double d) { return EPSILON < d && d < 1.0 - EPSILON; }
    @Override
    public boolean intersects(Rectangle rectangle) {
        if(rectangle instanceof Triangle2D) {
            // idea: new added point is C. existing points are at A and B that coincide with a face.
            // triangle can be described as triangle = C + (A - C) * u + (B - C) * v
            // if there is an intersecion, there will be one where u = 0 or when v = 0
            // existing triangle RST can be triangle can be described as existing = R + (S - R) * s + (T - R) * t
            // or existing = S + (R - S) * r + (T - S) * t; or existing = T + (R - T) * r + (S - T) * s

            Triangle2D triangle = (Triangle2D) rectangle;
            // check lines for intersection
            int sourceCount = (this.precomputedVals.length - LENGTHS_OFFSET) / LENGTHS_COUNT;
            int targetCount = (triangle.precomputedVals.length - LENGTHS_OFFSET) / LENGTHS_COUNT;
            for(int i = 0; i < sourceCount; i++) {
                int sourceStartPos = LENGTHS_OFFSET + LENGTHS_COUNT * i;
                double sourcePosX = precomputedVals[sourceStartPos + VAL_POS_SOURCE_X];
                double sourcePosY = precomputedVals[sourceStartPos + VAL_POS_SOURCE_Y];
                double sourceLenX = precomputedVals[sourceStartPos + VAL_LEN_X];
                double sourceLenY = precomputedVals[sourceStartPos + VAL_LEN_Y];

                for (int j = 0; j < targetCount; j++) {
                    int targetStartPos = LENGTHS_OFFSET + LENGTHS_COUNT * j;
                    double sourceToDestX = this.precomputedVals[targetStartPos + VAL_POS_SOURCE_X] - sourcePosX;
                    double sourceToDestY = this.precomputedVals[targetStartPos + VAL_POS_SOURCE_Y] - sourcePosY;

                    double targetLenX = triangle.precomputedVals[targetStartPos];
                    double targetLenY = triangle.precomputedVals[targetStartPos];
                    double denominator = (targetLenX * sourceLenY - targetLenY * sourceLenX);
                    if (FastMath.abs(denominator) < EPSILON) // lines are parallel
                        continue;

                    // NOTE: lines are parallel if (this.A.x - this.C.x) * (other.A.y - other.C.y) == (this.A.y - this.C.y) * (other.A.x - other.C.x)
                    // u = ((S_y - R_y) (C_x - R_x) - (S_x - R_x)(C_y - R_y)) / ((S_x - R_x)(A_y - C_y) - (S_y - R_y)(A_x - C_x))
                    // v = ((A_y - C_y) (C_x - R_x) - (A_x - C_x)(C_y - R_y)) / ((S_x - R_x)(A_y - C_y) - (S_y - R_y)(A_x - C_x))
                    // D = ((S_x - R_x)(A_y - C_y) - (S_y - R_y)(A_x - C_x))
                    double reciprocal = 1.0 / denominator;
                    double u = (targetLenY * sourceToDestX - targetLenX * sourceToDestY) * reciprocal;
                    double v = (sourceLenY * sourceToDestX - sourceLenX * sourceToDestY) * reciprocal;
                    if (inRangeExclusive(u) && inRangeExclusive(v) && inRangeExclusive(u + v)) {
                        return true;
                    }
                }
            }

            // if a point from here is in the other triangle, or if a point from the other triangle is in here
            // then the entire triangle may be encapsulated by the other one, without edges intersecting.
            // perform this check
            double otherX = triangle.precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_POS_SOURCE_X];
            double otherY = triangle.precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_POS_SOURCE_Y];
            double thisX = this.precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_POS_SOURCE_X];
            double thisY = this.precomputedVals[LENGTHS_OFFSET + LENGTHS_COUNT + VAL_POS_SOURCE_Y];

            return inTriangle(otherX, otherY) || triangle.inTriangle(thisX, thisY);
        } else {
            // otherwise, use the following method: https://seblee.me/2009/05/super-fast-trianglerectangle-intersection-test/
            // lines are {POS_SOURCE_X, POS_SOURCE_Y} + {VAL_LEN_X, VAL_LEN_Y} * u
            // how do we transform this into a basic linear equation?

            // 1. otherwise test if a point in this triangle is contained in rectangle, if true then entire triangle is within rectangle
            int edgeCount = (this.precomputedVals.length - LENGTHS_OFFSET) / LENGTHS_COUNT;
            for(int i = 0; i < edgeCount; i++) {
                int startPos = LENGTHS_OFFSET + LENGTHS_COUNT*i;
                double posX = precomputedVals[startPos + VAL_POS_SOURCE_X];
                double posY = precomputedVals[startPos + VAL_POS_SOURCE_Y];
                double lenX = precomputedVals[startPos + VAL_LEN_X];
                double lenY = precomputedVals[startPos + VAL_LEN_Y];

                if(FastMath.abs(lenX) < EPSILON) {
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

                    if(overlapRange(y0, y1 - y0, rectangle.y1(), rectangle.y2())) {
                        return true;
                    }
                }
            }

            // final test:
            // point from the rectangle. check if it's in bounds of this triangle. If it is,
            // then the rectangle is entirely contained within this triangle
            return inTriangle(rectangle.x1(), rectangle.y1());
        }
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

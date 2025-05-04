package io.hostilerobot.ceramicrelief.drivers.rtee;

import javafx.beans.property.*;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.shape.StrokeType;

import java.util.List;
import java.util.function.Consumer;

public class TriangleView extends Polygon {
    private BooleanProperty selected = new SimpleBooleanProperty(false);
    private BooleanProperty intersected = new SimpleBooleanProperty(false);

    public double getMinX() {
        double min = getVertex(A).getX();
        if(getVertex(B).getX() < min) min = getVertex(B).getX();
        if(getVertex(C).getX() < min) min = getVertex(C).getX();
        return min;
    }
    public double getMinY() {
        double min = getVertex(A).getY();
        if(getVertex(B).getY() < min) min = getVertex(B).getY();
        if(getVertex(C).getY() < min) min = getVertex(C).getY();
        return min;
    }
    public double getMaxX() {
        double max = getVertex(A).getX();
        if(getVertex(B).getX() > max) max = getVertex(B).getX();
        if(getVertex(C).getX() > max) max = getVertex(C).getX();
        return max;
    }
    public double getMaxY() {
        double max = getVertex(A).getY();
        if(getVertex(B).getY() > max) max = getVertex(B).getY();
        if(getVertex(C).getY() > max) max = getVertex(C).getY();
        return max;
    }


    private void setStyle(boolean intersected, boolean selected) {
        Paint stroke;
        if(selected) {
            strokeWidthProperty().set(4);
            stroke = Color.FIREBRICK;
        } else {
            strokeWidthProperty().set(2);
            stroke = Color.DARKSLATEBLUE;
        }

        if(intersected) {
            stroke = Color.RED;
            if(getStrokeDashArray().isEmpty())
                getStrokeDashArray().add(5d);
        } else if(!getStrokeDashArray().isEmpty()){
            getStrokeDashArray().clear();
        }
        setStroke(stroke);
    }

    private final static int STRIDE = 2;
    public final static int A = 0;
    public final static int B = 1;
    public final static int C = 2;

    public Point2D getVertex(int vertex) {
        return new Point2D(getVertexPositionXProperty(vertex).get(),
                getVertexPositionYProperty(vertex).get());
    }

    /**
     * does a calculation to see which vertices of this triangle are within epsilon distance of pointX, pointY
     * @param epsilon the distance our vertices are compared to, less than or equal to relation
     * @param pointX xPosition
     * @param pointY yPosition
     * @param vertices the vertices that are within the distance, appended to the list. Either A, B, or C
     * @return the number of vertices that are appended to {@param vertices}
     */
    public int touchingVertices(double epsilon,
                                double pointX, double pointY,
                                List<Integer> vertices) {
        // do the dot product from point to A, B, and C
        // and return the closest result that's within epsilon

        double minDistance = epsilon * epsilon;
        int count = 0;
        for(int idx = A; idx <= C; idx++) {
            double x = getCoordinatesX(idx);
            double y = getCoordinatesY(idx);
            double distanceX = pointX - x;
            double distanceY = pointY - y;
            double distance = distanceX * distanceX + distanceY * distanceY;
            if(distance <= minDistance) {
                count++;
                vertices.add(idx);
            }
        }

        return count;
    }

    // todo - possibly we could have these null, and lazily instantiate
    //        then later dispose when they're not being used
    private DoubleProperty vertexAXProperty;
    private DoubleProperty vertexAYProperty;

    private DoubleProperty vertexBXProperty;
    private DoubleProperty vertexBYProperty;

    private DoubleProperty vertexCXProperty;
    private DoubleProperty vertexCYProperty;

    private Consumer<? super Number> vertexSubscriptionX(int vertex) {
        int offset = vertex * STRIDE;
        return newX -> {
            getPoints().set(offset, (double)newX);
        };
    }
    private Consumer<? super Number> vertexSubscriptionY(int vertex) {
        int offset = vertex * STRIDE + 1;
        return newX -> {
            getPoints().set(offset, (double)newX);
        };
    }

    public TriangleView(Point2D a, Point2D b, Point2D c) {
        super(a.getX(), a.getY(), b.getX(), b.getY(), c.getX(), c.getY());
        setStrokeLineJoin(StrokeLineJoin.MITER);
        setStrokeType(StrokeType.INSIDE);
        setFill(Color.TRANSPARENT);
        vertexAXProperty = new SimpleDoubleProperty(a.getX());
        vertexAYProperty = new SimpleDoubleProperty(a.getY());
        vertexBXProperty = new SimpleDoubleProperty(b.getX());
        vertexBYProperty = new SimpleDoubleProperty(b.getY());
        vertexCXProperty = new SimpleDoubleProperty(c.getX());
        vertexCYProperty = new SimpleDoubleProperty(c.getY());

        vertexAXProperty.subscribe(vertexSubscriptionX(A));
        vertexAYProperty.subscribe(vertexSubscriptionY(A));
        vertexBXProperty.subscribe(vertexSubscriptionX(B));
        vertexBYProperty.subscribe(vertexSubscriptionY(B));
        vertexCXProperty.subscribe(vertexSubscriptionX(C));
        vertexCYProperty.subscribe(vertexSubscriptionY(C));

        setStyle(false, false);
        selected.subscribe(selected -> {
            setStyle(intersected.get(), selected);
        });
        intersected.subscribe(intersected -> {
            setStyle(intersected, selected.get());
        });
    }


    public BooleanProperty isSelectedProperty() {
        return selected;
    }
    public BooleanProperty isIntersectedProperty() {
        return intersected;
    }

    public DoubleProperty getVertexPositionXProperty(int vertex) {
        return switch (vertex) {
            case A -> vertexAXProperty;
            case B -> vertexBXProperty;
            case C -> vertexCXProperty;
            default -> null;
        };
    }
    public DoubleProperty getVertexPositionYProperty(int vertex) {
        return switch (vertex) {
            case A -> vertexAYProperty;
            case B -> vertexBYProperty;
            case C -> vertexCYProperty;
            default -> null;
        };
    }

    private double getCoordinatesX(int idx) {
        int offset = idx * STRIDE;
        return getPoints().get(offset);
    }
    private double getCoordinatesY(int idx) {
        int offset = idx * STRIDE + 1;
        return getPoints().get(offset);
    }
}

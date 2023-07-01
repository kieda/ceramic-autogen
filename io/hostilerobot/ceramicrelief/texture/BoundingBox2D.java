package io.hostilerobot.ceramicrelief.texture;

import javafx.geometry.Point2D;

public class BoundingBox2D{
    private double minX, maxX, minY, maxY;
    private BoundingBox2D(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    public static BoundingBox2D fromPoint(Point2D point) {
        return new BoundingBox2D(point.getX(), point.getY(), point.getX(), point.getY());
    }

    /**
     * expand the bounds of this bounding box to include point
     */
    void union(Point2D point) {
        if(point.getX() < minX) {
            minX = point.getX();
        } else if(point.getX() > maxX) {
            maxX = point.getX();
        }

        if(point.getY() < minY) {
            minY = point.getY();
        } else if(point.getY() > maxY) {
            maxY = point.getY();
        }
    }

    public double getMinX() {
        return minX;
    }
    public double getMinY() {
        return minY;
    }

    public double getMaxX() {
        return maxX;
    }
    public double getMaxY() {
        return maxY;
    }
}

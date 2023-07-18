package io.hostilerobot.ceramicrelief.texture.mesh_traversal;

import javafx.geometry.Point2D;

public class ProjectedTextureInfo {
    // the number of faces and vertices we added during this traversal
    private int tFaceCount = 0;
    private int tVertexCount = 0;
    // if null, no points added yet (empty bounding box)
    private BoundingBox2D bounds = null;

    ProjectedTextureInfo(){}

    public BoundingBox2D getBounds() {
        return bounds;
    }

    public int getTFaceCount() {
        return tFaceCount;
    }

    public int getTVertexCount() {
        return tVertexCount;
    }

    void addPoint(Point2D point) {
        if(bounds == null) {
            bounds = BoundingBox2D.fromPoint(point);
        } else {
            bounds.union(point);
        }
        tVertexCount++;
    }

    void addPoints(Point2D p1, Point2D p2, Point2D p3) {
        if(bounds == null) {
            bounds = BoundingBox2D.fromPoint(p1);
        } else {
            bounds.union(p1);
        }
        bounds.union(p2);
        bounds.union(p3);
        tVertexCount += 3;
    }

    void incrementFaceCount(){
        tFaceCount++;
    }
}

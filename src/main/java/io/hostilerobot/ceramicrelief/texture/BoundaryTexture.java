package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.imesh.IMesh;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.TriangleMesh;
import org.apache.commons.math.util.MathUtils;
import org.jgrapht.Graph;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;

// essentially represents a texture that
public class BoundaryTexture {
    private IMesh<? extends Object> backingMesh;
        // since we're not adding items we really don't care about the type of the backing ID, however this is still useful in lookups

    private static class TFace{
        private int v1;
        private int v2;
        private int v3;
    }

    // texture vertices
    private List<Point2D> tVertices;
    // our list of faces on the texture. Faces indexed to tVertices
    private List<TFace> tFaces;
    // bounding boxes for each face
    private List<Rectangle2D> boundingBoxes;
    //  ordered locations for bounding boxes, used to quickly
    private TreeSet<Integer> xBoundsMin;
    private TreeSet<Integer> xBoundsMax;
    private TreeSet<Integer> yBoundsMin;
    private TreeSet<Integer> yBoundsMax;



    public BoundaryTexture(IMesh<? extends Object> backingMesh) {
        tFaces = new ArrayList<>();
        boundingBoxes = new ArrayList<>();
        xBoundsMin = new TreeSet<>(Comparator.comparingDouble(x -> boundingBoxes.get(x).getMinX()));
        xBoundsMax = new TreeSet<>(Comparator.comparingDouble(x -> boundingBoxes.get(x).getMaxX()));
        yBoundsMin = new TreeSet<>(Comparator.comparingDouble(y -> boundingBoxes.get(y).getMinY()));
        yBoundsMax = new TreeSet<>(Comparator.comparingDouble(y -> boundingBoxes.get(y).getMaxY()));
    }

    private void addFace(TFace face) {
        Rectangle2D bounds = boundingBox(face);
        int insertIndex = tFaces.size();
        tFaces.add(face);
        boundingBoxes.add(bounds);
        xBoundsMin.add(insertIndex);
        xBoundsMax.add(insertIndex);
        yBoundsMin.add(insertIndex);
        yBoundsMax.add(insertIndex);
    }

    private Rectangle2D boundingBox(TFace face) {
        Point2D v1 = tVertices.get(face.v1);
        Point2D v2 = tVertices.get(face.v2);
        Point2D v3 = tVertices.get(face.v3);
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
        return new Rectangle2D(minX, minY, maxX - minX, maxY - minY);
    }
}

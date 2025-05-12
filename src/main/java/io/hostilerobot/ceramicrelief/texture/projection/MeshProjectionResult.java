package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.texture.TFace;
import javafx.geometry.Point2D;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * represents the resulting data of projecting a mesh to a texture
 *
 * has the following:
 *      tVertices: 2d vertices on the plane
 *      tFaces: A set of triangles, each formed by 3 indices in tVertices for their actual location
 *      faceMapping: information on which faces in 3d correspond to which faces in 2d
 *      textureConnections: graph describing which faces are adjacent in 3d (even if they might not be adjacent in 2d)
 */
public class MeshProjectionResult {
    private final List<TFace> tFaces;
    private final List<Point2D> tVertices;
    private final FaceMappingInfo faceMapping;
    private final Graph<TFace, EdgeInfo> textureConnections;

    public MeshProjectionResult(int faceCount, int vertexCount) {
        tFaces = new ArrayList<>(faceCount);
        tVertices = new ArrayList<>(vertexCount);
        faceMapping = new FaceMappingInfo(faceCount);
        textureConnections = new SimpleGraph<>(null, null, false);
    }

    public List<TFace> getTFaces() {
        return tFaces;
    }
    public List<Point2D> getTVertices() {
        return tVertices;
    }
    public FaceMappingInfo getFaceMapping() {
        return faceMapping;
    }
    public Graph<TFace, EdgeInfo> getTextureConnections() {
        return textureConnections;
    }
}

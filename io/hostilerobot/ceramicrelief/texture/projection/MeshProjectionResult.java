package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.texture.data_projection.EdgeInfo;
import io.hostilerobot.ceramicrelief.texture.data_projection.FaceMappingInfo;
import io.hostilerobot.ceramicrelief.texture.data_tex.TFace;
import javafx.geometry.Point2D;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.ArrayList;
import java.util.List;

/**
 * represents the resulting data of projecting a mesh to a texture
 */
public class MeshProjectionResult {
    private List<TFace> tFaces;
    private List<Point2D> tVertices;
    private FaceMappingInfo faceMapping;
    private Graph<TFace, EdgeInfo> textureConnections;

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

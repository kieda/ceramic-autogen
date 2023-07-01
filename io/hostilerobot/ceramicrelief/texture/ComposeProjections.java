package io.hostilerobot.ceramicrelief.texture;

import javafx.geometry.Point2D;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;

public class ComposeProjections {
    private Graph<TFace, EdgeInfo> textureConnections; // describes the relationship between triangles in our 2d texture
    // todo - make textureconnections graph (after the fact)
    public ComposeProjections() {
        this.textureConnections = new SimpleGraph<>(null, null, false);
    }
    public void translateProjections(
        List<Point2D> packing,
        ProjectMesh projection) {
        // based on the packing (which defines the new top-left corner for each bounding box in the traversal)
        // compose into one texture.
        // things we want to have as a result:
        // new list of vertices, new list of points
        // graph (V, E) describing the relation between TFaces
    }

}

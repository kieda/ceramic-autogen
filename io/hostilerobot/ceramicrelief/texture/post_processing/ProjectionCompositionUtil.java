package io.hostilerobot.ceramicrelief.texture.post_processing;

import io.hostilerobot.ceramicrelief.texture.data_projection.ProjectionState;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.ProjectedTextureInfo;
import javafx.geometry.Point2D;

import java.util.List;

public class ProjectionCompositionUtil {
    private ProjectionCompositionUtil() {}
    public static void translateProjections(List<ProjectedTextureInfo> textures, List<Point2D> translation, List<Point2D> vertices) {
        TranslateDisjointProjections.translate(textures, translation, vertices);
    }
    public static void generateTextureGraph(ProjectionState projectionState) {
        GenerateTextureGraph.process(projectionState);
    }
}

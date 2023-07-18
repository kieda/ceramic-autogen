package io.hostilerobot.ceramicrelief.texture.post_processing;

import io.hostilerobot.ceramicrelief.texture.mesh_traversal.ProjectedTextureInfo;
import javafx.geometry.Point2D;

import java.util.List;

class TranslateDisjointProjections {
    private TranslateDisjointProjections() {
    }

    static void translate(List<ProjectedTextureInfo> textures, List<Point2D> translation, List<Point2D> vertices) {
        assert textures.size() == translation.size();

        // based on the packing (which defines the new top-left corner for each bounding box in the traversal)
        // compose into one texture.
        // things we want to have as a result:
        // new list of vertices, new list of points
        int currentVertex = 0;
        for(int idx = 0; idx < textures.size(); idx++) {
            ProjectedTextureInfo textureInfo = textures.get(idx);
            Point2D topLeft = translation.get(idx);

            double translateX = textureInfo.getBounds().getMinX() - topLeft.getX();
            double translateY = textureInfo.getBounds().getMinY() - topLeft.getY();

            int vertexCount = textureInfo.getTVertexCount();
            int vertexEnd = currentVertex + vertexCount;
            for(int vert = currentVertex; vert < vertexEnd; vert++) {
                Point2D vertex = vertices.get(vert);
                Point2D translated = vertex.add(translateX, translateY);
                vertices.set(vert, translated);
            }

            currentVertex = vertexEnd;
        }
    }
}

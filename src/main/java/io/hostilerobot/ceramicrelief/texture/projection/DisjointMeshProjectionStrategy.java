package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.collection.TransformList;
import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.texture.TEdgeConnectionPolicy;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.BoundingBox2D;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.MeshPartitionTraversal;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.ProjectedTextureInfo;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.PartitionedMeshTraversal;
import io.hostilerobot.ceramicrelief.texture.packing.BoxPacker2D;
import io.hostilerobot.ceramicrelief.texture.post_processing.ProjectionCompositionUtil;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.Map;

/**
 * represents a projection strategy that projects a 3d mesh into 2d by using separate components.
 * It projects some disjoint subset of the mesh, then finally combines them into one holistic plane using box packing
 */
public class DisjointMeshProjectionStrategy implements MeshProjectionStrategy{
    private final PartitionedMeshTraversal projectAllFaces;
    private final BoxPacker2D packBoxes;

    public DisjointMeshProjectionStrategy(MeshPartitionTraversal projectSubset, BoxPacker2D packBoxes) {
        this.projectAllFaces = new PartitionedMeshTraversal(projectSubset);
        this.packBoxes = packBoxes;
    }

    @Override
    public MeshProjectionResult project(QMesh mesh, Map<QMeshEdge, TEdgeConnectionPolicy> initialPolicy) {
        // initial state. Only used internally
        ProjectionState state = new ProjectionState(mesh, initialPolicy);
        // project all subsets of the mesh
        List<ProjectedTextureInfo> textures = projectAllFaces.projectAll(state);
        // find a packing for the individual mesh projections
        List<BoundingBox2D> boundingBoxes = new TransformList<>(textures, ProjectedTextureInfo::getBounds);
        List<Point2D> packing = packBoxes.pack(boundingBoxes);
        // pack the individual mesh components
        ProjectionCompositionUtil.translateProjections(textures, packing, state.getProjection().getTVertices());
        // create a texture graph
        ProjectionCompositionUtil.generateTextureGraph(state);
        // return the result.
        return state.getProjection();
    }
}

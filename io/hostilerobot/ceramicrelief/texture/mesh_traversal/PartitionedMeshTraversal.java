package io.hostilerobot.ceramicrelief.texture.mesh_traversal;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.texture.projection.ProjectionState;
import io.hostilerobot.ceramicrelief.texture.projection.MeshProjectionResult;

import java.util.*;

/**
 * Represents functionality to project a 3d mesh in partitions. It does so by projecting the first face "f"
 * and all faces that are in the same set as the first. Then it finds the next face "f" that has not been projected yet
 * and projects that, and so on.
 */
public class PartitionedMeshTraversal {
    private final MeshPartitionTraversal projector;
    public PartitionedMeshTraversal(MeshPartitionTraversal projector) {
        this.projector = projector;
    }

    public List<ProjectedTextureInfo> projectAll(ProjectionState projectionState) {
        QMesh mesh = projectionState.getMesh();
        List<ProjectedTextureInfo> traversals = new ArrayList<>();
        MeshProjectionResult projection = projectionState.getProjection();
        mesh.getFaces().forEachOrdered(id -> {
            if(projection.getFaceMapping().isFacePlacedOnTexture(id))
                return;
            traversals.add(projector.projectSubset(id, projectionState));
        });

        // assert that all faces from this mesh are placed on the texture.
        assert mesh.getFaces().allMatch(projection.getFaceMapping()::isFacePlacedOnTexture);
        return traversals;
    }
}

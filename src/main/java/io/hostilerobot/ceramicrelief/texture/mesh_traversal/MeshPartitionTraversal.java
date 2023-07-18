package io.hostilerobot.ceramicrelief.texture.mesh_traversal;

import io.hostilerobot.ceramicrelief.texture.projection.ProjectionState;

/**
 * represents functionality to perform an individual traversal on a mesh partition starting at initialFace
 */
public interface MeshPartitionTraversal {
    ProjectedTextureInfo projectSubset(
            // inputs
            int initialFace,
            ProjectionState projectionState);
}

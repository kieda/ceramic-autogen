package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.texture.TEdgeConnectionPolicy;

import java.util.Map;

/**
 * represents the transformation for projecting a mesh to a texture
 */
public interface MeshProjectionStrategy {
    /**
     * projects a 3d mesh onto a 2d surface
     *
     * @param backingMesh the 3d mesh
     * @param initialPolicy the policy for connections in the mesh.
     *                      allows for seams so a texture isn't directly connected
     *                      allows for a seam to be contiguous with adjacent faces, or double back and be mirrored
     * @return the result of this projection
     */
    public MeshProjectionResult project(QMesh backingMesh, Map<QMeshEdge, TEdgeConnectionPolicy> initialPolicy);
}

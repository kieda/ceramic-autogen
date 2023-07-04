package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.texture.data_tex.TEdgeConnectionPolicy;

import java.util.Map;

/**
 * represents the transformation for projecting a mesh to a texture
 */
public interface MeshProjectionStrategy {
    public MeshProjectionResult project(QMesh backingMesh, Map<QMeshEdge, TEdgeConnectionPolicy> initialPolicy);
}

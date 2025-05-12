package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.texture.TEdgeConnectionPolicy;

import java.util.HashMap;
import java.util.Map;

/**
 * represents the state of projecting from 3d -> 2d as we're going through different steps
 *
 * has the following:
 *      mesh: the 3 dimensional mesh
 *      projection: the result, including any temporary or interim results
 *      connections: policy for how edges in 3d will form a 2d seam with neighbors.
 *                   May be modified or populated during the projection process.
 */
public class ProjectionState {
    private final QMesh mesh;
    private final MeshProjectionResult projection;
    private final Map<QMeshEdge, TEdgeConnectionPolicy> connections;

    public ProjectionState(QMesh mesh, Map<QMeshEdge, TEdgeConnectionPolicy> initialPolicy) {
        this.mesh = mesh;
        this.connections = new HashMap<>(initialPolicy);
        projection = new MeshProjectionResult(mesh.faceCount(), mesh.vertexCount());
    }

    public QMesh getMesh() {
        return mesh;
    }
    public MeshProjectionResult getProjection() {
        return projection;
    }
    public Map<QMeshEdge, TEdgeConnectionPolicy> getConnections() {
        return connections;
    }
}

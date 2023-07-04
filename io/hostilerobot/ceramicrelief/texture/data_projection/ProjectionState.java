package io.hostilerobot.ceramicrelief.texture.data_projection;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.texture.projection.MeshProjectionResult;
import io.hostilerobot.ceramicrelief.texture.data_tex.TEdgeConnectionPolicy;

import java.util.HashMap;
import java.util.Map;

public class ProjectionState {
    private QMesh mesh;
    private MeshProjectionResult projection;
    private Map<QMeshEdge, TEdgeConnectionPolicy> connections;

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

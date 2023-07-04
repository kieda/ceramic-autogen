package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.texture.data_tex.TEdgeConnectionPolicy;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.DFSMeshPartitionTraversal;
import io.hostilerobot.ceramicrelief.texture.packing.SquareBoxPacker2D;

import java.util.Map;

public class DFSMeshProjectionStrategy implements MeshProjectionStrategy{
    private final MeshProjectionStrategy strat;
    private DFSMeshProjectionStrategy() {
        strat = new DisjointMeshProjectionStrategy(new DFSMeshPartitionTraversal(),
                new SquareBoxPacker2D());
    }
    public static final DFSMeshProjectionStrategy INSTANCE = new DFSMeshProjectionStrategy();
    @Override
    public MeshProjectionResult project(QMesh backingMesh, Map<QMeshEdge, TEdgeConnectionPolicy> initialPolicy) {
        return strat.project(backingMesh, initialPolicy);
    }
}

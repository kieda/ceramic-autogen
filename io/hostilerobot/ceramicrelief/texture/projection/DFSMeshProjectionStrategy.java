package io.hostilerobot.ceramicrelief.texture.projection;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.texture.TEdgeConnectionPolicy;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.DFSMeshPartitionTraversal;
import io.hostilerobot.ceramicrelief.texture.packing.SquareBoxPacker2D;

import java.util.Map;

/**
 * default implementation: partition using DFS, pack using squareboxpacker
 */
public class DFSMeshProjectionStrategy implements MeshProjectionStrategy{
    public static final DFSMeshProjectionStrategy INSTANCE = new DFSMeshProjectionStrategy();
    private final MeshProjectionStrategy strat;
    private DFSMeshProjectionStrategy() {
        strat = new DisjointMeshProjectionStrategy(new DFSMeshPartitionTraversal(),
                new SquareBoxPacker2D());
    }

    @Override
    public MeshProjectionResult project(QMesh backingMesh, Map<QMeshEdge, TEdgeConnectionPolicy> initialPolicy) {
        return strat.project(backingMesh, initialPolicy);
    }
}

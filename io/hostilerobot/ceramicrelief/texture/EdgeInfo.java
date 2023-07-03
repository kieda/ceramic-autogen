package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.util.Hash;

/**
 * represents a mapping from a 3d mesh edge to a 2d one in the texture plane
 * a mesh is defined by the two faces that share two vertices
 * the texture EdgeInfo is defined by two edges in the texture plane, as an edge in the texture plane might not necessarily be adjacent
 * we also store the policy, which defines
 */
public class EdgeInfo {
    // represents the first edge on the 2d plane
    private TEdge edge1;
    // represents the second edge on the 2d plane
    private TEdge edge2;

    // edge1 and edge2 are considered connected according to policy, even if they are not directly adjacent in the 2d plane
    private TEdgeConnectionPolicy policy;

    // the edge on the mesh that directly represents this edge
    private QMeshEdge meshEdge;

    public EdgeInfo(TEdge edge1, TEdge edge2,
                    TEdgeConnectionPolicy policy,
                    QMeshEdge meshEdge) {
        this.edge1 = edge1;
        this.edge2 = edge2;
        this.policy = policy;
        this.meshEdge = meshEdge;
    }

    public boolean equals(Object other) {
        if(this == other)
            return true;
        if(other.getClass() != EdgeInfo.class)
            return false;
        EdgeInfo otherEdge = (EdgeInfo) other;

        if(!otherEdge.meshEdge.equals(meshEdge))
            return false;
        return (edge1.equals(otherEdge.edge1) && edge2.equals(otherEdge.edge2))
                || (edge1.equals(otherEdge.edge2) && edge2.equals(otherEdge.edge1));
    }

    public int hashCode() {
        int edgeHash = Hash.hashSymmetric(edge1.hashCode(), edge2.hashCode());
        int meshHash = meshEdge.hashCode();
        return Hash.hashOrdered(meshHash, edgeHash);
    }

    public TEdge getEdge1() {
        return edge1;
    }

    public TEdge getEdge2() {
        return edge2;
    }

    public QMeshEdge getMeshEdge() {
        return meshEdge;
    }

    public TEdgeConnectionPolicy getPolicy() {
        return policy;
    }
}

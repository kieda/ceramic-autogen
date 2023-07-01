package io.hostilerobot.ceramicrelief.texture;

import com.github.davidmoten.guavamini.Objects;
import io.hostilerobot.ceramicrelief.imesh.IMesh;
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
    private IMesh<Object>.IMeshEdge meshEdge;

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
        return meshHash * 31 + edgeHash;
    }
}

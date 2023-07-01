package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.imesh.IMesh;

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
        if(!(other instanceof EdgeInfo))
            return false;
        EdgeInfo otherEdge = (EdgeInfo) other;
        return false;
    }
}

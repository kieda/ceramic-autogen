package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.imesh.IMesh;

import java.util.Map;

/**
 * represents a traversal starting from an individual face in the IMesh, and places down as many adjacent faces as possible
 *
 * inputs:
 *      ID initialFace  -- face we start the traversal with
 *      IMesh backingMesh   -- graph of 3d faces
 *
 * outputs:
 *      List<TFace> tFaces      -- texture faces
 *      List<Point2D> tVertices -- texture vertices
 *      BoundingBox box         -- box that encapsulates all tFaces
 * modified/accumulated:
 *      Map<ID, FaceInfo> faceMapping -- mapping from the 3d face to its representation in tFaces
 *      Map<IMeshEdge, TEdgeConnectionPolicy> -- mapping from distinct 3d edges to how they should be connected in 2d
 */
public class MeshProjectionTraversal {
    // make this generic since we're not adding items we really don't care about the type of the backing ID type
    private final IMesh<? extends Object> backingMesh;
    private Map<IMesh<? extends Object>.IMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy;
//    private Map<Object, BoundaryTexture.FaceInfo> faceMapping;

    public MeshProjectionTraversal(
            Object initialFace,
            IMesh<? extends Object> backingMesh,
            Map<IMesh<? extends Object>.IMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy) {
        this.backingMesh = backingMesh;
        this.edgeConnectionPolicy = edgeConnectionPolicy;
    }
}

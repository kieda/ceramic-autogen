package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.imesh.IMesh;
import javafx.geometry.Point2D;

import java.util.*;

/**
 * projects a 3d mesh to 2d space. Does this by BFS traversal from each face.
 * Creates a series of contiguous polygons comprised of 2d triangles. Each polygon
 * represent a disjoint subset of the faces on the 3d mesh, such that all faces are placed.
 *
 * todo: refactor such that data is its own structure we can pass around, then we have an implementation function
 *       that transforms the data. Currently, doing all the work in the constructor is a dumb design pattern
 */
public class ProjectMesh {
    // initial mesh
    private final IMesh<Object> backingMesh;

    // the result of traversing through the mesh
    private final List<MeshProjectionTraversal> traversals;
    private final List<TFace> tFaces;
    private final List<Point2D> tVertices;
    private final Map<Object, FaceInfo> faceMapping;
    private final Map<IMesh<Object>.IMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy;

    public ProjectMesh(IMesh<Object> backingMesh) {
        this(new HashMap<>(), backingMesh);
    }
    public ProjectMesh(IMesh<Object> backingMesh,
        Map<IMesh<Object>.IMeshEdge, TEdgeConnectionPolicy> userPolicy) {
        this(new HashMap<>(userPolicy), backingMesh);
    }

    // private constructor, also sets all data structures
    private ProjectMesh(Map<IMesh<Object>.IMeshEdge, TEdgeConnectionPolicy> initialPolicy, IMesh<Object> backingMesh) {
        this.backingMesh = backingMesh;
        this.edgeConnectionPolicy = initialPolicy;
        this.traversals = new ArrayList<>();
        this.faceMapping = new HashMap<>();
        this.tVertices = new ArrayList<>();
        this.tFaces = new ArrayList<>();

        fillMapping();
        traverse();
    }

    /**
     * assigns int ids for each face in the mesh so we can have an ordering when deciding which face to traverse next
     */
    private void fillMapping() {
        Set<Object> ids = backingMesh.getFaces();
        if(ids.stream().allMatch(x -> x instanceof Integer)) {
            // just use the original mapping. Keep the user's ordering
            for(Object id : ids) {
                faceMapping.put(id, new FaceInfo((int)id));
            }
        } else {
            // otherwise user could be using strings and hand-constructing the shape. Use the order they were inserted in
            int iFacePos = 0;
            for(Object id : ids) {
                faceMapping.put(id, new FaceInfo(iFacePos++));
            }
        }
    }

    private void traverse() {
        for(Object id : backingMesh.getFaces()) {
            // mapping should already be made for all IDs in the constructor
            if (faceMapping.get(id).isFacePlacedOnTexture())
                continue;

            traversals.add(new MeshProjectionTraversal(id, backingMesh,
                    tFaces, tVertices,
                    faceMapping, edgeConnectionPolicy));
        }

        // assert that all faces from this mesh are placed on the texture.
        assert backingMesh.getFaces().stream().allMatch(id -> faceMapping.containsKey(id) && faceMapping.get(id).isFacePlacedOnTexture());
    }

    public List<MeshProjectionTraversal> getTraversals() {
        return traversals;
    }

    public IMesh<Object> getBackingMesh() {
        return backingMesh;
    }

    public List<Point2D> gettVertices() {
        return tVertices;
    }

    public List<TFace> gettFaces() {
        return tFaces;
    }

    public Map<IMesh<Object>.IMeshEdge, TEdgeConnectionPolicy> getEdgeConnectionPolicy() {
        return edgeConnectionPolicy;
    }

    public Map<Object, FaceInfo> getFaceMapping() {
        return faceMapping;
    }
}

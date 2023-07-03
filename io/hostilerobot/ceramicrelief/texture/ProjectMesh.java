package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
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
    private final QMesh backingMesh;

    // the result of traversing through the mesh
    private final List<MeshProjectionTraversal> traversals;
    private final List<TFace> tFaces;
    private final List<Point2D> tVertices;
    private final FaceMappingInfo faceMapping;
    private final Map<QMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy;

    public ProjectMesh(QMesh backingMesh) {
        this(new HashMap<>(), backingMesh);
    }
    public ProjectMesh(QMesh backingMesh,
                       Map<QMeshEdge, TEdgeConnectionPolicy> userPolicy) {
        this(new HashMap<>(userPolicy), backingMesh);
    }

    // private constructor, also sets all data structures
    private ProjectMesh(Map<QMeshEdge, TEdgeConnectionPolicy> initialPolicy, QMesh backingMesh) {
        this.backingMesh = backingMesh;
        this.edgeConnectionPolicy = initialPolicy;
        this.traversals = new ArrayList<>();
        this.faceMapping = new FaceMappingInfo(backingMesh.faceCount());
        this.tVertices = new ArrayList<>();
        this.tFaces = new ArrayList<>();

        traverse();
    }

    private void traverse() {
        backingMesh.getFaces().forEachOrdered(id -> {
            if(faceMapping.isFacePlacedOnTexture(id))
                return;
            traversals.add(new MeshProjectionTraversal(id, backingMesh,
                    tFaces, tVertices,
                    faceMapping, edgeConnectionPolicy));
        });

        // assert that all faces from this mesh are placed on the texture.
        assert backingMesh.getFaces().allMatch(faceMapping::isFacePlacedOnTexture);
    }

    public List<MeshProjectionTraversal> getTraversals() {
        return traversals;
    }

    public QMesh getBackingMesh() {
        return backingMesh;
    }

    public List<Point2D> getTVertices() {
        return tVertices;
    }

    public List<TFace> getTFaces() {
        return tFaces;
    }

    public Map<QMeshEdge, TEdgeConnectionPolicy> getEdgeConnectionPolicy() {
        return edgeConnectionPolicy;
    }

    public FaceMappingInfo getFaceMapping() {
        return faceMapping;
    }
}

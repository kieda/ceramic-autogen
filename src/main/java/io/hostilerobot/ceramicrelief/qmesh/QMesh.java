package io.hostilerobot.ceramicrelief.qmesh;

import io.hostilerobot.ceramicrelief.util.Hash;
import org.apache.commons.math.fraction.Fraction;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
import java.util.function.BiFunction;
import java.util.stream.IntStream;

/**
 * Intermediate mesh representation that provides a bit more functionality than a series of points
 * represents a simple triangle based mesh that we can utilize for mug construction
 * does not have any information on textures, which would be added in another step
 *
 * has additional utility for tracking the edges of items
 *
 * We use an int as an id for the vertices and the edges since it works nicely with arrays and bitsets
 * We used to have a generic parametric type ID that the user could have if they wanted to label using strings
 * But I think it's better to just have a hashmap that is exposed to the client so the heavier lifting is just done with faster data structures.
 */
public class QMesh {
    /**
     * cache mesh edge. Used to find edges that are the same in 3d space, and only used internally in IMesh
     * to build a graph among faces that share an edge. Note that more than two faces may share an edge in 3d space
     */
    private static class CMeshEdge {
        // represents IDs for vertices in the mesh
        private int v1, v2;

        private CMeshEdge(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CMeshEdge that = (CMeshEdge) o;

            // it's the same edge if we're switching up the order
            return (getV1() == that.getV2() && getV2() == that.getV2())
                    || (getV1() == that.getV2() && getV2() == that.getV1());
        }

        public int getV1() {
            return v1;
        }

        public int getV2() {
            return v2;
        }

        @Override
        public int hashCode() {
            return Hash.hashSymmetric(getV1(), getV2());
        }

        public String toString() {
            return getV1() + " -- " + getV2();
        }
    }

    public int faceCount() {
        return faces.size();
    }
    public int vertexCount() {
        return vertices.size();
    }
    public IntStream getFaces() {
        return IntStream.range(0, faceCount());
    }
    public IntStream getVertices() {
        return IntStream.range(0, vertexCount());
    }

    private List<Map<Integer, CMeshEdge>> edgeCache; // cache the existing edges so we'll use the same objects
    private Map<CMeshEdge, Set<Integer>> edgeConnectivity; // keep track of edges and the faces that are attached to it

    private List<QVertex3D> vertices;
    private List<QMeshFace> faces;
    private List<QVertex3D> normals; // map from the face ID to the normal for the face

    private Graph<Integer, QMeshEdge> meshConnectivity;
        // auto-built graph that keeps track of the connectivity along edges of faces
        // V: Face tag
        // E: Edge in the mesh, represented by two vertex tags.

    public QMesh() {
        // make the following linked
        this.vertices = new ArrayList<>();
        this.faces = new ArrayList<>();
        this.meshConnectivity = new SimpleGraph<>(null, null, false);
        this.edgeConnectivity = new HashMap<>();
        this.edgeCache = new ArrayList<>();
        this.normals = new ArrayList<>();
    }
    private static <X> void expandToIdx(List<X> items, int idx) {
        int toAdd = idx - items.size() + 1;
        for(int x = 0; x < toAdd; x++)
            items.add(null);
    }

    private CMeshEdge getCachedEdge(int v1, int v2) {
        // swap so v1 is always the lesser element.
        if(v1 > v2) {
            int tmp = v1;
            v1 = v2;
            v2 = tmp;
        }

        Map<Integer, CMeshEdge> entry = null;
        if(v1 < edgeCache.size()) {
            CMeshEdge edge;
            if((entry = edgeCache.get(v1)) != null && (edge = entry.get(v2)) != null) {
                return edge;
            }
        } else {
            // expand edgeCache to include v1
            expandToIdx(edgeCache, v1);
        }
        if(entry == null) {
            entry = new HashMap<>(2);
            edgeCache.set(v1, entry);
        }
        CMeshEdge edge = new CMeshEdge(v1, v2);
        entry.put(v2, edge);
        return edge;
    }

    public Graph<Integer, QMeshEdge> getMeshConnectivity() {
        return meshConnectivity;
    }

    public QVertex3D getVertex(int id) {
        return vertices.get(id);
    }
    public QMeshFace getFace(int id) {
        return faces.get(id);
    }

    private boolean validVertex(int id) {
        return id >= 0 && id < vertexCount();
    }

    // normal that has not (yet) changed to a unit vector
    public QVertex3D getNormal(int face) {
        QVertex3D normal;

        if (face < normals.size() && (normal = normals.get(face)) != null) {
            return normal;
        } else {
            QMeshFace meshFace = getFace(face);
            QVertex3D len12 = new QVertex3D();
            QVertex3D len23 = new QVertex3D();

            // return (vertex2 - vertex1) x (vertex3 - vertex2)
            QVertex3D vertex2 = getVertex(meshFace.getV2());
            QVertex3D.subtract(vertex2, getVertex(meshFace.getV1()), len12);
            QVertex3D.subtract(getVertex(meshFace.getV3()), vertex2, len23);
            QVertex3D.cross(len12, len23, len23);
            expandToIdx(normals, face);
            normals.set(face, len23);
            return len23;
        }
    }

    public boolean setVertex(int id, double x, double y, double z) {
        if(!validVertex(id)) {
            return false;
        }
        vertices.get(id).set(x, y, z);
        return true;
    }

    public int addVertex(double x, double y, double z) {
        int idx = vertexCount();
        vertices.add(new QVertex3D(x, y, z));
        return idx;
    }

    /**
     * @param v1 the ID for the first vertex
     * @param v2 the ID for the second vertex
     * @param v3 the ID for the third vertex
     * @return id for the new triangle
     */
    public int addTriangle(int v1, int v2, int v3) {
        if(!validVertex(v1) || !validVertex(v2) || !validVertex(v3)) {
            throw new NoSuchElementException("Mesh does not contain all vertices (" + v1 + ", " + v2 + ", " + v3 + ")");
        }
        final int idx = faceCount();
        faces.add(new QMeshFace(v1, v2, v3));
        meshConnectivity.addVertex(idx); // add a vertex for the face.

        BiFunction<CMeshEdge, Set<Integer>, Set<Integer>> addFaceFn = (edge, faces) -> {
            if(faces == null) {
                faces = new HashSet<>();
            }
            // add graph connectivity from this face to all other faces that share an edge
            for(int face : faces) {
                // don't place an edge down in a self loop to the same face
                // don't add an edge between faces that are already connected
                if(face != idx) {
                    if(!meshConnectivity.containsEdge(idx, face)) {
                        meshConnectivity.addEdge(idx, face, new QMeshEdge(idx, face));
                    }
                    if(!meshConnectivity.containsEdge(face, idx)) {
                        // bidirectional support in case we want to swap out the graph type
                        meshConnectivity.addEdge(face, idx, new QMeshEdge(face, idx));
                    }
                }
            }
            faces.add(idx);
            return faces;
        };

        // if there already is a face with this edge, then we add this face to the set of faces along this edge
        // we also add a connection in the graph to represent that the faces share an edge
        edgeConnectivity.compute(getCachedEdge(v1, v2), addFaceFn);
        edgeConnectivity.compute(getCachedEdge(v2, v3), addFaceFn);
        edgeConnectivity.compute(getCachedEdge(v3, v1), addFaceFn);

        return idx;
    }

    public void setVertices(List<QVertex3D> vertices, List<QMeshFace> faces) {
        // todo
    }
}

package io.hostilerobot.ceramicrelief.imesh;


import org.apache.commons.math.fraction.Fraction;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.*;
import java.util.function.BiFunction;

/**
 * Intermediate mesh representation that provides a bit more functionality than a series of points
 * represents a simple triangle based mesh that we can utilize for mug construction
 * does not have any information on textures, which would be added in another step
 *
 * has additional utility for tracking the edges of items
 *
 * Use generic ID - if we want to have a string representation for built objects vs integer for generated
 */
public class IMesh<ID> {
    /**
     * cache mesh edge. Used to find edges that are the same in 3d space, and only used internally in IMesh
     * to build a graph among faces that share an edge. Note that more than two faces may share an edge in 3d space
     */
    private class CMeshEdge {
        // represents IDs for vertices in the mesh
        private ID v1, v2;

        private CMeshEdge(ID v1, ID v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CMeshEdge that = (CMeshEdge) o;

            // it's the same edge if we're switching up the order
            return (getV1().equals(that.getV2()) && getV2().equals(that.getV2()))
                    || (getV1().equals(that.getV2()) && getV2().equals(that.getV1()));
        }

        public ID getV1() {
            return v1;
        }

        public ID getV2() {
            return v2;
        }

        @Override
        public int hashCode() {
            return getV1().hashCode() * getV2().hashCode();
        }

        public String toString() {
            return getV1() + " -- " + getV2();
        }
    }

    public class IMeshEdge {
        // represents IDs for two adjacent edges in the mesh
        // this is more specific than CMeshEdge, as more than two faces in the mesh can share an edge.
        // However, we disallow two faces from sharing more than one edge
        private final ID face1, face2;

        public IMeshEdge(ID face1, ID face2) {
            this.face1 = face1;
            this.face2 = face2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IMeshEdge that = (IMeshEdge) o;

            // it's the same edge if we're switching up the order
            return (getFace1().equals(that.getFace2()) && getFace2().equals(that.getFace2()))
                    || (getFace1().equals(that.getFace2()) && getFace2().equals(that.getFace1()));
        }

        public ID getFace1() {
            return face1;
        }
        public ID getFace2() {
            return face2;
        }
        public ID getOtherFace(ID face) {
            if(face == getFace1()) {
                return getFace2();
            } else {
                assert face == getFace2();
                return getFace1();
            }
        }

        @Override
        public int hashCode() {
            return getFace1().hashCode() * getFace2().hashCode();
        }
        public String toString() {
            return getFace1() + " -- " + getFace2();
        }
    }
    public class IMeshFace {
        // id for the face in the mesh
        private final ID face;
        // ids for vertices of the face
        private final ID v1;
        private final ID v2;
        private final ID v3;
        private IMeshFace(ID face, ID v1, ID v2, ID v3) {
            this.face = face; // this face's ID
            this.v1 = v1;
            this.v2 = v2;
            this.v3 = v3;
        }
        public ID getV1() {
            return v1;
        }

        public ID getV2() {
            return v2;
        }

        public ID getV3() {
            return v3;
        }
        public IVertex3D getVertex1() {
            return vertices.get(getV1());
        }
        public IVertex3D getVertex2() {
            return vertices.get(getV2());
        }
        public IVertex3D getVertex3() {
            return vertices.get(getV3());
        }

        // normal that has not (yet) changed to a unit vector
        public IVertex3D getNormal() {
            IVertex3D normal;

            if((normal = normals.get(face)) != null) {
                return normal;
            } else {
                IVertex3D len12 = new IVertex3D();
                IVertex3D len23 = new IVertex3D();

                // return (vertex2 - vertex1) x (vertex3 - vertex2)
                IVertex3D.subtract(getVertex2(), getVertex1(), len12);
                IVertex3D.subtract(getVertex3(), getVertex2(), len23);
                IVertex3D.cross(len12, len23, len23);
                return len23;
            }
        }
    }

    public Set<ID> getFaces() {
        return faces.keySet();
    }

    private Map<ID, Map<ID, CMeshEdge>> edgeCache; // cache the existing edges so we'll use the same objects
    private Map<CMeshEdge, Set<ID>> edgeConnectivity; // keep track of edges and the faces that are attached to it

    private Map<ID, IVertex3D> vertices;
    private Map<ID, IMeshFace> faces;
    private Map<ID, IVertex3D> normals; // map from the face ID to the normal for the face

    private Graph<ID, IMeshEdge> meshConnectivity;
        // auto-built graph that keeps track of the connectivity along edges of faces
        // V: Face tag
        // E: Edge in the mesh, represented by two vertex tags.

    public IMesh() {
        // make the following linked
        this.vertices = new LinkedHashMap<>();
        this.faces = new LinkedHashMap<>();
        this.meshConnectivity = new SimpleGraph<>(null, null, false);
        this.edgeConnectivity = new HashMap<>();
        this.edgeCache = new HashMap<>();
        this.normals = new HashMap<>();
    }

    private CMeshEdge getCachedEdge(ID v1, ID v2) {
        Map<ID, CMeshEdge> entry1, entry2;
        CMeshEdge edge;
        if((entry1 = edgeCache.get(v1)) != null && (edge = entry1.get(v2)) != null) {
            return edge;
        } else if((entry2 = edgeCache.get(v2)) != null && (edge = entry2.get(v2)) != null) {
            return edge;
        } else {
            edge = new CMeshEdge(v1, v2);
            if(entry1 != null) {
                entry1.put(v2, edge);
            } else if(entry2 != null) {
                entry2.put(v1, edge);
            } else {
                Map<ID, CMeshEdge> newEntry = new HashMap<>(2);
                newEntry.put(v2, edge);
                edgeCache.put(v1, newEntry);
            }
            return edge;
        }
    }

    public Graph<ID, IMeshEdge> getMeshConnectivity() {
        return meshConnectivity;
    }

    public IVertex3D getVertex(ID id) {
        return vertices.get(id);
    }
    public IMeshFace getFace(ID id) {
        return faces.get(id);
    }

    public IMesh<ID> addVertex(ID id, Fraction x, Fraction y, Fraction z) {
        return addVertex(id, new IVertex3D(x, y, z));
    }

    public IMesh<ID> addVertex(ID id, IVertex3D vertex) {
        if(edgeConnectivity == null) {
            throw new IllegalStateException("Cannot add new vertices to an IMesh that has been cleaned");
        }
        vertices.put(id, vertex);
        return this;
    }

    /**
     * @param id the ID to be used for the first triangle
     * @param v1 the ID for the first vertex
     * @param v2 the ID for the second vertex
     * @param v3 the ID for the third vertex
     * @return this Mesh
     */
    public IMesh<ID> addTriangle(ID id, ID v1, ID v2, ID v3) {
        if(edgeConnectivity == null) {
            throw new IllegalStateException("Cannot add a new triangle to an IMesh that has been cleaned");
        }
        if(!vertices.containsKey(v1) || !vertices.containsKey(v2) || !vertices.containsKey(v3)) {
            throw new NoSuchElementException("Mesh does not contain all vertices (" + v1 + ", " + v2 + ", " + v3 + ")");
        }

        faces.put(id, new IMeshFace(id, v1, v2, v3));
        meshConnectivity.addVertex(id); // add a vertex for the face.

        BiFunction<CMeshEdge, Set<ID>, Set<ID>> addFaceFn = (edge, faces) -> {
            if(faces == null) {
                faces = new HashSet<>();
            }
            // add graph connectivity from this face to all other faces that share an edge
            for(ID face : faces) {
                // don't place an edge down in a self loop to the same face
                // don't add an edge between faces that are already connected
                if(!Objects.equals(face, id)) {
                    if(!meshConnectivity.containsEdge(id, face)) {
                        meshConnectivity.addEdge(id, face, new IMeshEdge(id, face));
                    }
                    if(!meshConnectivity.containsEdge(face, id)) {
                        // bidirectional support in case we want to swap out the graph type
                        meshConnectivity.addEdge(face, id, new IMeshEdge(face, id));
                    }
                }
            }
            faces.add(id);
            return faces;
        };

        // if there already is a face with this edge, then we add this face to the set of faces along this edge
        // we also add a connection in the graph to represent that the faces share an edge
        edgeConnectivity.compute(getCachedEdge(v1, v2), addFaceFn);
        edgeConnectivity.compute(getCachedEdge(v2, v3), addFaceFn);
        edgeConnectivity.compute(getCachedEdge(v3, v1), addFaceFn);

        return this;
    }

    public void clean() {
        edgeConnectivity.clear();
        edgeConnectivity = null;
    }
}

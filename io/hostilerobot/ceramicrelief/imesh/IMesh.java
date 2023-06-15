package io.hostilerobot.ceramicrelief.imesh;


import org.apache.commons.math.fraction.Fraction;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
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
    public class IMeshEdge {
        // represents IDs for vertices in the mesh
        private ID v1, v2;

        private IMeshEdge(ID v1, ID v2) {
            this.v1 = v1;
            this.v2 = v2;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IMeshEdge that = (IMeshEdge) o;

            // it's the same edge if we're switching up the order
            return (getVertex1().equals(that.getVertex2()) && getVertex2().equals(that.getVertex2()))
                || (getVertex1().equals(that.getVertex2()) && getVertex2().equals(that.getVertex1()));
        }

        public ID getVertex1() {
            return v1;
        }
        public ID getVertex2() {
            return v2;
        }

        IMeshEdge setVertices(ID v1, ID v2) {
            this.v1 = v1;
            this.v2 = v2;
            return this;
        }

        @Override
        public int hashCode() {
            return getVertex1().hashCode() * getVertex2().hashCode();
        }

        public String toString() {
            return getVertex1() + " -- " + getVertex2();
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
        public IMeshEdge getEdge1_2() {
            return getCachedEdge(getV1(), getV2());
        }
        public IMeshEdge getEdge2_3() {
            return getCachedEdge(getV2(), getV3());
        }
        public IMeshEdge getEdge3_1() {
            return getCachedEdge(getV3(), getV1());
        }

        private Set<IMeshEdge> outgoing = null;
        public Set<IMeshEdge> getOutgoingEdges() {
            if(outgoing == null) {
                outgoing = meshConnectivity.outgoingEdgesOf(face);
            }
            return outgoing;
        }
        private List<ID> neighborFaces = null;
        public List<ID> getNeighborFaces() {
            if(neighborFaces == null) {
                neighborFaces = Graphs.neighborListOf(meshConnectivity, face);
            }
            return neighborFaces;
        }
    }

    public Set<ID> getFaces() {
        return faces.keySet();
    }

    private Map<ID, Map<ID, IMeshEdge>> edgeCache; // cache the existing edges so we'll use the same objects
    private Map<IMeshEdge, Set<ID>> edgeConnectivity; // keep track of edges and the faces that are attached to it
    private Map<ID, IVertex3D> vertices;
    private Map<ID, IMeshFace> faces;
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
    }

    private IMeshEdge getCachedEdge(ID v1, ID v2) {
        Map<ID, IMeshEdge> entry1, entry2;
        IMeshEdge edge;
        if((entry1 = edgeCache.get(v1)) != null && (edge = entry1.get(v2)) != null) {
            return edge;
        } else if((entry2 = edgeCache.get(v2)) != null && (edge = entry2.get(v2)) != null) {
            return edge;
        } else {
            edge = new IMeshEdge(v1, v2);
            if(entry1 != null) {
                entry1.put(v2, edge);
            } else if(entry2 != null) {
                entry2.put(v1, edge);
            } else {
                Map<ID, IMeshEdge> newEntry = new HashMap<>(2);
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
        vertices.put(id, vertex);
        return this;
    }

    public IMesh<ID> addTriangle(ID id, ID v1, ID v2, ID v3) {
        if(!vertices.containsKey(v1) || !vertices.containsKey(v2) || !vertices.containsKey(v3)) {
            throw new NoSuchElementException("Mesh does not contain all vertices (" + v1 + ", " + v2 + ", " + v3 + ")");
        }

        faces.put(id, new IMeshFace(id, v1, v2, v3));
        meshConnectivity.addVertex(id); // add a vertex for the face.

        BiFunction<IMeshEdge, Set<ID>, Set<ID>> addFaceFn = (edge, faces) -> {
            if(faces == null) {
                faces = new HashSet<>();
            }
            // add graph connectivity from this face to all other faces that share an edge
            for(ID face : faces) {
                if(!Objects.equals(face, id)) {
                    meshConnectivity.addEdge(face, id, edge);
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
}

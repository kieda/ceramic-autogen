package io.hostilerobot.ceramicrelief.texture;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.SearchRTree;
import com.github.davidmoten.rtree2.geometry.internal.GeometryUtil;
import io.hostilerobot.ceramicrelief.imesh.IMesh;
import io.hostilerobot.ceramicrelief.imesh.IVertex3D;
import javafx.geometry.Point2D;
import org.apache.commons.math.fraction.Fraction;
import org.apache.commons.math.util.FastMath;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;

import static org.jheaps.AddressableHeap.Handle;

import org.jgrapht.graph.SimpleGraph;
import org.jheaps.tree.PairingHeap;

import java.util.*;


// rather than managing a bunch of stuff for treesets and keeping the bounding boxes this way,
// we can just use a RTree library. Then I don't have to fix as many bugs
// essentially represents a texture that
public class BoundaryTexture {
    private static class TEdge {
        public TEdge(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
        private int v1;
        private int v2;

        public int getV1() { return v1; }
        public int getV2() {
            return v2;
        }
    }
    private static class TFace{
        private int v1;
        private int v2;
        private int v3;

        public int getV1() {
            return v1;
        }
        public int getV3() {
            return v3;
        }
        public int getV2() {
            return v2;
        }
    }


    private static void populateEdge1_2(IMesh.IMeshFace faceToAdd, HeapElem reference) {
        IVertex3D v2 = faceToAdd.getVertex2();
        // we create these vector in the same direction that they are defined
        // such that they are increasing e.g. 1->2, 2->3, or 3->1
        // triVector goes from the end of the edgeVector to the new point
        reference.edgeVector = v2.subtract(faceToAdd.getVertex1());
        reference.triVector = faceToAdd.getVertex3().subtract(v2);
        reference.vertexToPlace = faceToAdd.getV3();
    }

    private static void populateEdge2_3(IMesh.IMeshFace faceToAdd, HeapElem reference) {
        IVertex3D v3 = faceToAdd.getVertex3();
        reference.edgeVector = v3.subtract(faceToAdd.getVertex2());
        reference.triVector = faceToAdd.getVertex1().subtract(v3);
        reference.vertexToPlace = faceToAdd.getV1();
    }

    private static void populateEdge3_1(IMesh.IMeshFace faceToAdd, HeapElem reference) {
        IVertex3D v1 = faceToAdd.getVertex1();
        reference.edgeVector = v1.subtract(faceToAdd.getVertex3());
        reference.triVector = faceToAdd.getVertex2().subtract(v1);
        reference.vertexToPlace = faceToAdd.getV2();
    }

    private static void populateTEdge3_1Same(TFace face, HeapElem reference) {
        reference.isWindingSame = true;
        reference.oppositeTVertex = face.getV2();
        reference.edge2d = new TEdge(face.getV1(), face.getV3());
    }
    private static void populateTEdge3_1Diff(TFace face, HeapElem reference) {
        reference.isWindingSame = false;
        reference.oppositeTVertex = face.getV2();
        reference.edge2d = new TEdge(face.getV3(), face.getV1());
    }
    private static void populateTEdge1_2Same(TFace face, HeapElem reference) {
        reference.isWindingSame = true;
        reference.oppositeTVertex = face.getV3();
        reference.edge2d = new TEdge(face.getV2(), face.getV1());
    }
    private static void populateTEdge1_2Diff(TFace face, HeapElem reference) {
        reference.isWindingSame = false;
        reference.oppositeTVertex = face.getV3();
        reference.edge2d = new TEdge(face.getV1(), face.getV2());
    }
    private static void populateTEdge2_3Same(TFace face, HeapElem reference) {
        reference.isWindingSame = true;
        reference.oppositeTVertex = face.getV1();
        reference.edge2d = new TEdge(face.getV3(), face.getV2());
    }
    private static void populateTEdge2_3Diff(TFace face, HeapElem reference) {
        reference.isWindingSame = false;
        reference.oppositeTVertex = face.getV1();
        reference.edge2d = new TEdge(face.getV2(), face.getV3());
    }
    /**
     * not-so-elegant solution for finding a common face between two edges and finding where the expected texture coordinates should be
     * (accounting for winding order, etc). Also calculates which vertex on the second face we will be placing down
     *
     * extracts the common edge of the two faces based on comparing the reference IDs of the vertices
     * then it translates it to the edge of the texture coordinates by extracting it from {@param firstFace}, which represents
     * the coordinates for the first face.
     */
    private void prepareForPlacement(IMesh.IMeshFace first, IMesh.IMeshFace second, TFace firstFace,
                                     HeapElem reference) {
        // we only need to check currentFace.v1 and currentFace.v2
        // since they're sharing an edge, either one or the other will match
        if(first.getV1() == second.getV1()) {
            if(first.getV3() == second.getV2()) { // edge first[1--3] == second[1--2]
                populateTEdge3_1Same(firstFace, reference);
                populateEdge1_2(second, reference);
            } else if(first.getV2() == second.getV3()) { // edge first[1--2] == second[1--3]
                populateTEdge1_2Same(firstFace, reference);
                populateEdge3_1(second, reference);
            } // opposite windings
            else if(first.getV2() == second.getV2()) { // edge first[1--2] == second[1--2]
                populateTEdge1_2Diff(firstFace, reference);
                populateEdge1_2(second, reference);
            } else if(first.getV3() == second.getV3()) { // edge first[1--3] == second[1--3]
                populateTEdge3_1Diff(firstFace, reference);
                populateEdge3_1(second, reference);
            }
        } else if(first.getV1() == second.getV2()) {
            if(first.getV2() == second.getV1()) { // edge first[1--2] == second[2--1]
                populateTEdge1_2Same(firstFace, reference);
                populateEdge1_2(second, reference);
            } else if(first.getV3() == second.getV3()) { // edge first[1--3] == second[2--3]
                populateTEdge3_1Same(firstFace, reference);
                populateEdge2_3(second, reference);
            } // different winding conditions
            else if(first.getV2() == second.getV3()) { // edge first[1--2] == second[2--3]
                populateTEdge1_2Diff(firstFace, reference);
                populateEdge2_3(second, reference);
            } else if(first.getV3() == second.getV1()) { // edge first[1--3] == second[2--1]
                populateTEdge3_1Diff(firstFace, reference);
                populateEdge1_2(second, reference);
            }
        } else if(first.getV1() == second.getV3()) {
            if(first.getV2() == second.getV2()) { // edge first[1--2] == second[3--2]
                populateTEdge1_2Same(firstFace, reference);
                populateEdge2_3(second, reference);
            } else if(first.getV3() == second.getV1()) { // edge first[1--3] == second[3--1]
                populateTEdge3_1Same(firstFace, reference);
                populateEdge3_1(second, reference);
            } // opposite windings
            else if(first.getV2() == second.getV1()) { // edge first[1--2] == second[3--1]
                populateTEdge1_2Diff(firstFace, reference);
                populateEdge3_1(second, reference);
            } else if(first.getV3() == second.getV2()) { // edge first[1--3] == second[3--2]
                populateTEdge3_1Diff(firstFace, reference);
                populateEdge2_3(second, reference);
            }
        } else if(first.getV2() == second.getV1()) {
            if(first.getV3() == second.getV3()) { // edge first[2--3] == second[1--3]
                populateTEdge2_3Same(firstFace, reference);
                populateEdge3_1(second, reference);
            } // opposite winding
            else if(first.getV3() == second.getV2()) { // edge first[2--3] == second[1--2]
                populateTEdge2_3Diff(firstFace, reference);
                populateEdge1_2(second, reference);
            }
        } else if(first.getV2() == second.getV2()) {
            if(first.getV3() == second.getV1()) { // edge first[2--3] == second[2--1]
                populateTEdge2_3Same(firstFace, reference);
                populateEdge1_2(second, reference);
            } // opposite winding
            else if(first.getV3() == second.getV3()) { // edge first[2--3] == second[2--3]
                populateTEdge2_3Diff(firstFace, reference);
                populateEdge2_3(second, reference);
            }
        } else if(first.getV2() == second.getV3()) {
            if(first.getV3() == second.getV2()) { // edge first[2--3] == second[3--2]
                populateTEdge2_3Same(firstFace, reference);
                populateEdge2_3(second, reference);
            } // opposite winding
            else if(first.getV3() == second.getV1()) { // edge first[2--3] == second[3--1]
                populateTEdge2_3Diff(firstFace, reference);
                populateEdge3_1(second, reference);
            }
        }
        // these should be set by the end
        assert reference.edge2d != null && reference.edgeVector != null && reference.triVector != null;

        // populate policy: set it to default if it's not already defined
        TEdgeConnectionPolicy policy = edgeConnectionPolicy.get(reference.adjacentFace);
        reference.connectionPolicy = policy != null ? policy : TEdgeConnectionPolicy.getDefaultPolicy(reference.isWindingSame);

        assert reference.connectionPolicy != null; // policy should be set too.
    }

    private static class FaceInfo{
        private int tFace = -1; // the position in the texture. If negative, position is TBD
        private int faceOrder; // this is used internally as a method to distinguish 3d faces in the heap during our traversal
        public FaceInfo(int faceOrder) {
            this.faceOrder = faceOrder;
        }
        private void setTFace(int face) {
            this.tFace = face;
        }
        public boolean isFacePlacedOnTexture() {
            return tFace >= 0;
        }
    }
    private static class TEdgeInfo {
        // faces can be obtained using textureConnections.getSourceVertex/getTargetVertex
        TEdge edge;
        TEdgeConnectionPolicy policy;
    }

    // "close enough" distance for two points to be considered equal to another
    // things might not be exact due to the nature of double floating point errors
    private final static double EPSILON = 0.001;
    private final static double EPSILON_SQ = EPSILON * EPSILON;

    // since we're not adding items we really don't care about the type of the backing ID, however this is still useful in lookups
    private final IMesh<? extends Object> backingMesh;

    /**
     * describes how specific edges should be placed from 3d to 2d.
     * This is useful if we want to ensure that specific edges (that are connected in 3d) are not wrapped or connected in 2d.
     * This is also useful for describing how we should handle triangles in the 2d plane, for example if we encounter two triangles with different winding orders
     *
     * if an entry does not exist in the map, then we use TEdgeConnectionPolicy.getDefaultPolicy for a given edge
     */
    private Map<IMesh<? extends Object>.IMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy;
    
    // texture vertices
    private List<Point2D> tVertices;
    // our list of faces on the texture. Faces indexed to tVertices
    private List<TFace> tFaces;
    private RTree<TFace, Triangle2D> intersectionTest;

    private Map<Object, FaceInfo> faceMapping; // mapping of 3d face ID to index of 2d representation and internal index for 3d representation
    private Graph<TFace, TEdgeInfo> textureConnections; // describes the relationship between triangles in our 2d texture
            // todo - make textureconnections graph (after the fact)

    private void fillMapping() {
        Set<? extends Object> ids = backingMesh.getFaces();
        if(ids.stream().allMatch(x -> x instanceof Integer)) {
            // just use the original mapping. Keep the user's ordering
            for(Object id : ids) {
                faceMapping.put(id, new FaceInfo((int)id));
            }
        } else {
            // otherwise user could be using strings and hand-constructing the shape. Use the order they were inserted in
            int iFacePos = 0;
            for(Object id : backingMesh.getFaces()) {
                faceMapping.put(id, new FaceInfo(iFacePos++));
            }
        }
    }

    public BoundaryTexture(IMesh<? extends Object> backingMesh) {
        this(backingMesh, Collections.emptyMap());
    }

    /**
     * what we want to achieve :
     *   project a face from IMesh to 2d plane
     *   place it so it's at (0,0) for the first face, or adjacent to the existing face
     *
     *
     *   add 2dFace to the boundaryTexture by either placing it at (0,0) first face
     *   or adjacent to an existing face. However, before we place it down we check to see if it intersects with any faces in the RTree
     *   if it does not, then add the face to the current 2d triangle texture and to the RTree.
     *      Mark the face as "placed" and build a mapping from 3d triangle to 2d
     *   if it does, then mark the face as "dead", meaning this vertex is impassible in our current face traversal and will be saved for a future iteration
     */
    public BoundaryTexture(IMesh<? extends Object> backingMesh, Map<IMesh<? extends Object>.IMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy) {
        this.tFaces = new ArrayList<>();
        this.tVertices = new ArrayList<>();
        this.faceMapping = new HashMap<>();
        this.backingMesh = backingMesh;
        this.edgeConnectionPolicy = edgeConnectionPolicy;
        this.textureConnections = new SimpleGraph<>(null, null, false);

        // make an integer mapping for the faces in the mesh from Object ID -> int
        // Object ID is already the type of int, then we keep the original mapping.
        // however,
        fillMapping();
    }

    private static class HeapElem{
        private Object vertexToPlace;
        private Object faceId; // id of the face in 3d we're attempting to place down
        private IMesh<Object>.IMeshEdge adjacentFace; // edge in 3d that connects to this face (or null if it's the first item)
        private TEdge edge2d;  // edge in 2d that connects to the face (this is the translation from edge3d to the 2d plain)
        private IVertex3D edgeVector; // vector that describes the adjacent edge we're laying down
        private IVertex3D triVector; // vector that describes the new triangle we're laying down
        private TEdgeConnectionPolicy connectionPolicy;
        private boolean isWindingSame; // is the winding the same between the two edges?
        private int oppositeTVertex; // tVertex on the opposite side from edge2d
        public HeapElem(Object faceId) {
            this.faceId = faceId;
            edge2d = null;
            adjacentFace = null;
        }
        public HeapElem(Object faceId, IMesh<Object>.IMeshEdge adjacentFace) {
            this.faceId = faceId;
            this.adjacentFace = adjacentFace;
        }
        public boolean canBeConnected() {
            return connectionPolicy.connected(isWindingSame);
        }
        public boolean placeAllVertices() {
            return edge2d == null;
        }
        public Object getVertexToPlace() {
            return vertexToPlace;
        }
    }

    // as we traverse the 3d graph, we place down faces that are similar enough in angle to faces that are already down first
    // we break ties by choosing the lowest ID face to be placed
    // we break ties by choosing the lowest connecting edge in 2d
    private class HeapOrder {
        // constructor should only be utilized when inserting the first item into the heap
        public HeapOrder(Object faceId) {
            this.theta = -1;
            this.faceId = faceId;
            this.connectingEdge = null;
        }
        public HeapOrder(double theta, Object faceId, TEdge connectingEdge) {
            // faceId, connectingEdge must not be null.
            this.theta = theta;
            this.faceId = faceId;
            this.connectingEdge = connectingEdge;
        }
        private double theta; // A . B = ||A|| ||B|| cos(theta)
                      // A . B / ||A|| ||B|| = cos(theta)
                      // dot product of two normals on two faces = cos(theta). 1 if they are parallel
                      // 0 if they are perpendicular. -1 if they are in opposite direction.
                      // therefore, for ordering purposes, we order by -cos(theta) since this is a min-ordered heap
        // used to break ties.
        private Object faceId; // id of the face we're placing down
        private TEdge connectingEdge; // 2d edge in texture that connects to the face we're attempting to place down (null if there is none)

        public double getTheta() {
            return theta;
        }
        public Object getFaceId() {
            return faceId;
        }
        public int getFaceIndex() {
	        return faceMapping.get(getFaceId()).faceOrder;
        }

        public TEdge getConnectingEdge() {
            return connectingEdge;
        }
    }

    private static final Comparator<HeapOrder> HEAP_ORDER_COMPARATOR =  Comparator.comparingDouble(HeapOrder::getTheta)
            .thenComparingInt(HeapOrder::getFaceIndex)
            .thenComparing(HeapOrder::getConnectingEdge, Comparator.nullsFirst(Comparator.comparingInt(TEdge::getV1).thenComparing(TEdge::getV2)));

    // heap we use for traversal
    private PairingHeap<HeapOrder, HeapElem> heap;
    // replaced with FaceInfo mapping

    public void traverseFaces() {
        heap = new PairingHeap<>(HEAP_ORDER_COMPARATOR);

        IMesh<Object> backingMesh = (IMesh<Object>) this.backingMesh;
        Graph<Object, IMesh<Object>.IMeshEdge> connectivity = backingMesh.getMeshConnectivity();

        for(Object id : backingMesh.getFaces()) {
            // mapping should already be made for all IDs in the constructor
            if(faceMapping.get(id).isFacePlacedOnTexture())
                continue;

            // start with an empty intersection test each time we begin traversing the mesh.
            this.intersectionTest = RTree.create();

            // add an initial element
            HeapOrder initialOrder = new HeapOrder(id);
            HeapElem initialElem = new HeapElem(id);
            heap.insert(initialOrder, initialElem);

            while(!heap.isEmpty()) {
                // get an item off the heap
                Handle<HeapOrder, HeapElem> item = heap.deleteMin();
                HeapElem elem = item.getValue();
                Object currentFaceId = elem.faceId;

                // we already placed this item from the heap.
                // note we may have duplicates of the 3d face in the heap if we add it along different connecting edges
                if(faceMapping.get(currentFaceId).isFacePlacedOnTexture()) {
                    continue;
                }

                IMesh.IMeshFace faceToPlace = backingMesh.getFace(currentFaceId);

                TEdge adjacentEdge2d = elem.edge2d;

                Point2D insertedPoint = null; // non-null if this isn't the first inserted triangle
                Point2D p1, p2, p3; // three points we will place down (and test against existing items in the RTree

                // project the triangle onto the 2d plane, preserving the length of the sides
                // calculation is mostly the same between placing the first triangle and placing subsequent triangles
                //
                {
                    IVertex3D v12;
                    if(!elem.placeAllVertices()) {
                        assert faceMapping.get(elem.adjacentFace.getOtherFace(faceToPlace)).isFacePlacedOnTexture();
                            // the existing face must already be placed on the texture.

                        assert elem.canBeConnected(); // we should only add edges to the heap that can be connected
                        // this lines up with p1 -> p2
                        v12 = elem.edgeVector;
                    } else {
                        v12 = faceToPlace.getVertex2().subtract(faceToPlace.getVertex1());
                    }

                    double len12 = v12.length();

                    // X in unit vector
                    double u12x = v12.getX().doubleValue() / len12;
                    double u12y = v12.getY().doubleValue() / len12;
                    double u12z = v12.getZ().doubleValue() / len12;

                    // Z in orthonormal basis
                    IVertex3D normal = faceToPlace.getNormal();
                    double lenNormal = normal.length();

                    // Z in unit vector
                    double uNormalx = normal.getX().doubleValue() / lenNormal;
                    double uNormaly = normal.getY().doubleValue() / lenNormal;
                    double uNormalz = normal.getZ().doubleValue() / lenNormal;

                    // Y in orthonormal basis, where Y = X x Z
                    // this is already a unit vector, as X and Z are units.
                    double xC = (u12y*uNormalz) - (u12z*uNormaly);
                    double yC = (u12z*uNormalx) - (u12x*uNormalz);
                    double zC = (u12x*uNormaly) - (u12y*uNormalx);

                    IVertex3D v31;
                    if(!elem.placeAllVertices()) {
                        v31 = elem.triVector;
                    } else {
                        // we can reuse v12, as we don't need to use it any longer
                        IVertex3D.subtract(faceToPlace.getVertex1(), faceToPlace.getVertex3(), v12);
                        v31 = v12;
                    }

                    // first vertex is at (0,0), second is at (len12, 0), third is at (t3x, t3y)
                    // where t3x = (v1 - v3) . unit(X), t3y = (v1 - v3) . unit(Y)
                    double t3x, t3y;
                    {
                        double v31x = v31.getX().doubleValue();
                        double v31y = v31.getY().doubleValue();
                        double v31z = v31.getZ().doubleValue();
                        t3x = u12x*v31x + u12y*v31y + u12z*v31z;
                        t3y = xC*v31x + yC*v31y + zC*v31z;
                    }

                    if(!elem.placeAllVertices()) {
                        // new triangle shares an edge with an existing triangle in the texture, defined by p1p2.
                        p1 = tVertices.get(adjacentEdge2d.getV1());
                        p2 = tVertices.get(adjacentEdge2d.getV2());

                        // 2d vector from p1 to p2
                        double p1p2x = p2.getX() - p1.getX();
                        double p1p2y = p2.getY() - p1.getY();

                        // p3 = p1 + t3x/len12 * p1p2 + t3y/len12 * perp(p1p2)
                        // where perp([p1p2x, p1p2y]) = [-p1p2y, p1p2x] OR [p1p2y, -p1p2x]
                        double jx, jy;
                        {
                            double multx = t3x/len12;
                            jx = multx * p1p2x;
                            jy = multx * p1p2y;
                        }
                        double kx, ky;
                        {
                            double multy = t3y / len12;
                            kx = multy * p1p2y;
                            ky = multy * p1p2x; // note x and y are swapped because we are getting perpendicular vectors
                        }

                        // this is the vector p1p3
                        double p1p3x = jx - kx;
                        double p1p3y = jy + ky;
                        Point2D opposite = tVertices.get(elem.oppositeTVertex);
                        // opposite side test: see if p1p3 is on the opposite side of p1p2 from elem.oppositeTVertex
                        // this is done by using sign(p1p2 x p1p3) != sign(p1p2 x p1opposite)
                        double p1oppositex = opposite.getX() - p1.getX();
                        double p1oppositey = opposite.getY() - p1.getY();

                        double cross12_13 = p1p2x * p1p3y - p1p2y * p1p3x;
                        double cross12_1opposite = p1p2x * p1oppositey - p1p2y * p1oppositex;

                        if(FastMath.signum(cross12_13) != FastMath.signum(cross12_1opposite)) {
                            p3 = new Point2D(p1.getX() + p1p3x, p1.getY() + p1p3y);
                        } else {
                            // choose the opposite direction so it can lay flat, away from oppositeTVertex on the texture plane
                            p3 = new Point2D(p1.getX() + jx + kx, p1.getY() + jy - ky);
                        }

                        // note : we should not care about the connectionPolicy to determine the direction
                        // if the edges have opposite windings and is not mirrored, it wouldn't be added to the heap anyway
                        // if the edges have the same windings and is mirrored, it wouldn't be added to the heap
                        // thus, the only cases we may have when we reach this condition is mirrored and opposite windings (which we will automatically reverse)
                        // or adjacent and the same windings (which will have the original direction)
                        // as we will ensure the new point is on the opposite side of the edge from the existing point
                    } else {
                        p1 = Point2D.ZERO;
                        p2 = new Point2D(len12, 0.0);
                        p3 = new Point2D(t3x, t3y);
                    }
                }

                Triangle2D newTriangle = new Triangle2D(insertedPoint, p1, p2, p3);

                Iterable<Entry<TFace, Triangle2D>> entries = SearchRTree.search(intersectionTest, newTriangle);
                if(entries.iterator().hasNext()) {
                    // there is an intersection with the new triangle we're attempting to place down
                    // (we don't exactly care with what triangle)
                    // therefore we don't try to place down this triangle as it would overlap, and we continue with the next item in the heap
                    continue;
                }

                // otherwise place this face down.
                // add the new 2d vertex to our list and link it up with newFace
                // we search other adjacent faces, see if they are already on the texture map, and get the relevant coordinates if so.
                TFace newFace = new TFace();
                if(!elem.placeAllVertices()) {
                    Integer commonTVertex = null;
                    double currentMinDistance = 0; // set this for java compiler. However this is guaranteed to be set before it is read from.
                    for(IMesh<Object>.IMeshEdge outgoing : connectivity.iterables().edgesOf(currentFaceId)) {
                        Object otherFaceId = Graphs.getOppositeVertex(connectivity, outgoing, currentFaceId);
                        // graph connection from otherFaceId to currentFaceId
                        FaceInfo faceInfo = faceMapping.get(otherFaceId);
                        if(faceInfo.isFacePlacedOnTexture()) {
                            TFace adjacentTexture = tFaces.get(faceInfo.tFace);

                            // if the neighboring face is already placed on the texture, we may can consolidate the
                            // new point to an existing one if they're very close together
                            IMesh<Object>.IMeshFace adjacentFace = backingMesh.getFace(otherFaceId);
                            // find vertex in common with insertedPoint

                            int adjacentTIndex;
                            if(adjacentFace.getV1() == elem.vertexToPlace) {
                                adjacentTIndex = adjacentTexture.getV1();
                            } else if(adjacentFace.getV2() == elem.vertexToPlace) {
                                adjacentTIndex = adjacentTexture.getV2();
                            } else if(adjacentFace.getV3() == elem.vertexToPlace) {
                                adjacentTIndex = adjacentTexture.getV3();
                            } else{
                                continue;
                            }

                            Point2D adjacentTVertex = tVertices.get(adjacentTIndex);
                            double currentDistance = GeometryUtil.distanceSquared(adjacentTVertex.getX(), adjacentTVertex.getY(),
                                    insertedPoint.getX(), insertedPoint.getY());
                            // ensure that the common vertex that we find is actually close to the new 2d vertex we're attempting to lay down
                            if((commonTVertex == null && currentDistance <= EPSILON_SQ)
                                || (commonTVertex != null && currentDistance < currentMinDistance)) {
                                commonTVertex = adjacentTIndex;
                                currentMinDistance = currentDistance;
                            }
                        }
                    }

                    int newIndex;
                    if(commonTVertex == null) {
                        // we did not find a vertex that is already on the 2d texture map
                        // thus we add a new point to our array.
                        newIndex = tVertices.size();
                        tVertices.add(insertedPoint);
                    } else {
                        // we found a vertex that is already on the 2d texture map
                        // so we have it map to an existing item in our array.
                        newIndex = commonTVertex;
                    }

                    // we ensure that v1, v2, and v3 are defined in the original order as in IMesh.
                    if(elem.vertexToPlace == faceToPlace.getV1()) {
                        newFace.v1 = newIndex;
                        newFace.v2 = adjacentEdge2d.v1;
                        newFace.v3 = adjacentEdge2d.v2;
                    } else if(elem.vertexToPlace == faceToPlace.getV2()) {
                        newFace.v1 = adjacentEdge2d.v2;
                        newFace.v2 = newIndex;
                        newFace.v3 = adjacentEdge2d.v1;
                    } else if(elem.vertexToPlace == faceToPlace.getV3()) {
                        newFace.v1 = adjacentEdge2d.v1;
                        newFace.v2 = adjacentEdge2d.v2;
                        newFace.v3 = newIndex;
                    } else {
                        throw new IllegalStateException(elem.vertexToPlace + " not in " + faceToPlace);
                    }

                } else {
                    // we are placing the first triangle in a mesh subset
                    // we add all three vertices to the data structure, then add the face
                    int tri1 = tVertices.size();
                    newFace.v1 = tri1;
                    newFace.v2 = tri1 + 1;
                    newFace.v3 = tri1 + 2;

                    tVertices.add(p1);
                    tVertices.add(p2);
                    tVertices.add(p3);
                }

                // add the new face to our list of faces
                int newFaceId = tFaces.size();
                faceMapping.get(currentFaceId).tFace = newFaceId;
                tFaces.add(newFace);
                intersectionTest = intersectionTest.add(newFace, newTriangle);

                // run through the edges that connect to the current one
                for(IMesh<Object>.IMeshEdge edge : connectivity.iterables().edgesOf(currentFaceId)) {
                    // id of the other face
                    Object otherFaceId = Graphs.getOppositeVertex(connectivity, edge, currentFaceId);
                    // this face is already placed on the texture. We don't process it.
                    if(faceMapping.get(otherFaceId).isFacePlacedOnTexture())
                        continue;
                    IMesh.IMeshFace currentFace = backingMesh.getFace(currentFaceId);
                    IMesh.IMeshFace otherFace = backingMesh.getFace(otherFaceId);

                    // traverse to the new face (eventually) by putting it onto the heap
                    HeapElem newElem = new HeapElem(otherFaceId, edge);
                    prepareForPlacement(currentFace, otherFace, newFace, newElem); // populate information for the HeapElem so it can be placed on the 2d texture
                    // populate HeapElem such that we map edge3d to the appropriate edge2d, following winding orders

                    // if this face inherently can't be connected (e.g. by defined winding rules, or from user specification)
                    // then we don't add it to the heap or place it in the same tiling
                    if(!newElem.canBeConnected()) {
                        continue;
                    }

                    // current: A, B, C
                    // other: D, E, F
                    // (B - A) x (C - B) = normalCurrent
                    // (E - D) x (F - E) = normalOther
                    // normalCurrent . normalOther = ||normalCurrent|| * ||normalOther|| cos(theta)
                    double ranking = getRanking(currentFace, otherFace);
                    HeapOrder newElemOrder = new HeapOrder(ranking, otherFaceId, newElem.edge2d);

                    heap.insert(newElemOrder, newElem);
                }
            }

            // todo - we inserted all the faces we could. We clear out our existing caches, make a bounding box around it,
            //   and add it to a list of boxes we will place.
        }
    }

    private double getRanking(IMesh.IMeshFace currentFace, IMesh.IMeshFace otherFace) {
        // simpler method: return -cos(theta), which is calculated from the dot product of the two normals
        IVertex3D n1 = currentFace.getNormal();
        IVertex3D n2 = otherFace.getNormal();
        Fraction dotProd = n1.dot(n2);
        double vectorLength = Math.sqrt(n1.dot(n1).multiply(n2.dot(n2)).doubleValue());
        // return -cosTheta. This has the desired effect of being the highest value at theta PI and -PI, and lowest value at theta 0
        return -(dotProd.doubleValue() / vectorLength);
    }
}

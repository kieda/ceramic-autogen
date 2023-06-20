package io.hostilerobot.ceramicrelief.texture;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.SearchRTree;
import io.hostilerobot.ceramicrelief.imesh.IMesh;
import io.hostilerobot.ceramicrelief.imesh.IVertex3D;
import javafx.geometry.Point2D;
import org.apache.commons.math.fraction.Fraction;
import org.apache.commons.math.util.FastMath;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;

import static org.jheaps.AddressableHeap.Handle;
import org.jheaps.tree.PairingHeap;

import java.lang.ref.Reference;
import java.util.*;


// rather than managing a bunch of stuff for treesets and keeping the bounding boxes this way,
// we can just use a RTree library. Then I don't have to fix as many bugs
// essentially represents a texture that
public class BoundaryTexture {
    private IMesh<? extends Object> backingMesh;
        // since we're not adding items we really don't care about the type of the backing ID, however this is still useful in lookups

    private static class TEdge {
        public TEdge(int v1, int v2) {
            this.v1 = v1;
            this.v2 = v2;
        }
        private int v1;
        private int v2;

        public int getV1() {
            return v1;
        }

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

    private static boolean sanityCheck(
        IMesh.IMeshEdge edgeToCheck,
        Object vertex1,
        Object vertex2
    ) {
        return (edgeToCheck.getVertex1() == vertex1 && edgeToCheck.getVertex2() == vertex2) ||
                (edgeToCheck.getVertex2() == vertex1  && edgeToCheck.getVertex1() == vertex2);
    }

    /**
     * not-so-elegant solution for finding a common face between two edges and finding where the expected texture coordinates should be
     * (accounting for winding order, etc). Also calculates which vertex on the second face we will be placing down
     *
     * extracts the common edge of the two faces based on comparing the reference IDs of the vertices
     * then it translates it to the edge of the texture coordinates by extracting it from {@param firstFace}, which represents
     * the coordinates for the first face.
     *
     * todo - move to HeapElem. Some sort of populate function based on first, second, and firstFace.
     */
    private static void setCommonEdge_ref(IMesh.IMeshFace first, IMesh.IMeshFace second, TFace firstFace,
                                           HeapElem reference) {
        // we only need to check currentFace.v1 and currentFace.v2
        // since they're sharing an edge, either one or the other will match
        if(first.getV1() == second.getV1()) {
            if(first.getV3() == second.getV2()) { // edge first[1->3] == second[1->2]
                // sanity check that edge3d is in fact the right edge
                assert sanityCheck(reference.edge3d, second.getV1(), second.getV2());
                reference.otherVertexId = second.getV3();
                // keep edge2d consistent with edge3d
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV1(), firstFace.getV3()) :
                        new TEdge(firstFace.getV3(), firstFace.getV1());
            } else if(first.getV2() == second.getV3()) {
                assert sanityCheck(reference.edge3d, second.getV1(), second.getV3());
                reference.otherVertexId = second.getV2();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV1(), firstFace.getV2()) :
                        new TEdge(firstFace.getV2(), firstFace.getV1());
            } else if(first.getV2() == second.getV2()) {
                assert sanityCheck(reference.edge3d, second.getV1(), second.getV2());
                // !!! looks like we encountered different winding along adjacent edge.
                // we flip the edge
                // (alternative solution: we could also just continue and place this face
                // so it's disconnected)
                reference.otherVertexId = second.getV3();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV2(), firstFace.getV1()) :
                        new TEdge(firstFace.getV1(), firstFace.getV2());
            } else if(first.getV3() == second.getV3()) {
                assert sanityCheck(reference.edge3d, second.getV1(), second.getV3());
                // !!! same as above
                reference.otherVertexId = second.getV2();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV3(), firstFace.getV1()) :
                        new TEdge(firstFace.getV1(), firstFace.getV3());
            }
        } else if(first.getV1() == second.getV2()) {
            if(first.getV2() == second.getV1()) {
                assert sanityCheck(reference.edge3d, second.getV2(), second.getV1());
                reference.otherVertexId = second.getV3();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV1(), firstFace.getV2()) :
                        new TEdge(firstFace.getV2(), firstFace.getV1());
            } else if(first.getV3() == second.getV3()) {
                assert sanityCheck(reference.edge3d, second.getV2(), second.getV3());
                reference.otherVertexId = second.getV1();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV1(), firstFace.getV3()) :
                        new TEdge(firstFace.getV3(), firstFace.getV1());
            } // different winding conditions
            else if(first.getV2() == second.getV3()) {
                assert sanityCheck(reference.edge3d, second.getV2(), second.getV3());
                reference.otherVertexId = second.getV1();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV2(), firstFace.getV1()) :
                        new TEdge(firstFace.getV1(), firstFace.getV2());
            } else if(first.getV3() == second.getV1()) {
                assert sanityCheck(reference.edge3d, second.getV2(), second.getV1());
                reference.otherVertexId = second.getV3();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV3(), firstFace.getV1()) :
                        new TEdge(firstFace.getV1(), firstFace.getV3());
            }
        } else if(first.getV1() == second.getV3()) {
            if(first.getV2() == second.getV2()) {
                assert sanityCheck(reference.edge3d, second.getV3(), second.getV2());
                reference.otherVertexId = second.getV1();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV1(), firstFace.getV2()) :
                        new TEdge(firstFace.getV2(), firstFace.getV1());
            } else if(first.getV3() == second.getV1()) {
                assert sanityCheck(reference.edge3d, second.getV3(), second.getV1());
                reference.otherVertexId = second.getV2();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV1(), firstFace.getV3()) :
                        new TEdge(firstFace.getV3(), firstFace.getV1());
            } // opposite windings
            else if(first.getV2() == second.getV1()) {
                assert sanityCheck(reference.edge3d, second.getV3(), second.getV1());
                reference.otherVertexId = second.getV2();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV2(), firstFace.getV1()) :
                        new TEdge(firstFace.getV1(), firstFace.getV2());
            } else if(first.getV3() == second.getV2()) {
                assert sanityCheck(reference.edge3d, second.getV3(), second.getV2());
                reference.otherVertexId = second.getV1();
                reference.edge2d = (first.getV1() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV3(), firstFace.getV1()) :
                        new TEdge(firstFace.getV1(), firstFace.getV3());
            }
        } else if(first.getV2() == second.getV1()) {
            if(first.getV3() == second.getV3()) {
                assert sanityCheck(reference.edge3d, second.getV1(), second.getV3());
                reference.otherVertexId = second.getV2();
                reference.edge2d = (first.getV2() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV2(), firstFace.getV3()) :
                        new TEdge(firstFace.getV3(), firstFace.getV2());
            } // opposite winding
            else if(first.getV3() == second.getV2()) {
                assert sanityCheck(reference.edge3d, second.getV1(), second.getV2());
                reference.otherVertexId = second.getV3();
                reference.edge2d = (first.getV2() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV3(), firstFace.getV2()) :
                        new TEdge(firstFace.getV2(), firstFace.getV3());
            }
        } else if(first.getV2() == second.getV2()) {
            if(first.getV3() == second.getV1()) {
                assert sanityCheck(reference.edge3d, second.getV2(), second.getV1());
                reference.otherVertexId = second.getV3();
                reference.edge2d = (first.getV2() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV2(), firstFace.getV3()) :
                        new TEdge(firstFace.getV3(), firstFace.getV2());

            } // opposite winding
            else if(first.getV3() == second.getV3()) {
                assert sanityCheck(reference.edge3d, second.getV2(), second.getV3());
                reference.otherVertexId = second.getV1();
                reference.edge2d = (first.getV2() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV3(), firstFace.getV2()) :
                        new TEdge(firstFace.getV2(), firstFace.getV3());
            }
        } else if(first.getV2() == second.getV3()) {
            if(first.getV3() == second.getV2()) {
                assert sanityCheck(reference.edge3d, second.getV3(), second.getV2());
                reference.otherVertexId = second.getV1();
                reference.edge2d = (first.getV2() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV2(), firstFace.getV3()) :
                        new TEdge(firstFace.getV3(), firstFace.getV2());
            } // opposite winding
            else if(first.getV3() == second.getV1()) {
                assert sanityCheck(reference.edge3d, second.getV3(), second.getV1());
                reference.otherVertexId = second.getV2();
                reference.edge2d = (first.getV2() == reference.edge3d.getVertex1()) ?
                        new TEdge(firstFace.getV3(), firstFace.getV2()) :
                        new TEdge(firstFace.getV2(), firstFace.getV3());
            }
        }
    }

    private static class FaceInfo{
        private int tFace = -1; // the position in the texture. If negative, position is TBD
        private int iFace; // this is used internally as a method to distinguish 3d faces in the heap during our traversal
        public FaceInfo(int iFace) {
            this.iFace = iFace;
        }
        private void setTFace(int face) {
            this.tFace = face;
        }
        public boolean isFacePlacedOnTexture() {
            return tFace >= 0;
        }
    }
    // texture vertices
    private List<Point2D> tVertices;
    // XXX - this does not work. There might be multiple places where a 3d vertex maps to a 2d texture coordinate
//    private Map<Object, Integer> vertexMapping; // mapping of 3d vertex ID to index of 2d representation in our vertices
    // our list of faces on the texture. Faces indexed to tVertices
    private List<TFace> tFaces;
    private RTree<TFace, Triangle2D> intersectionTest;
    private Map<Object, FaceInfo> faceMapping; // mapping of 3d face ID to index of 2d representation and internal index for 3d representation


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
    public BoundaryTexture(IMesh<? extends Object> backingMesh) {
        this.tFaces = new ArrayList<>();
        this.tVertices = new ArrayList<>();
        this.backingMesh = backingMesh;
        this.faceMapping = new HashMap<>();
//        this.vertexMapping = new HashMap<>();

        // make an integer mapping for the faces in the mesh from Object ID -> int
        // Object ID is already the type of int, then we keep the original mapping.
        // however,
        fillMapping();
    }

    private static class HeapElem{
        private Object faceId; // id of the face in 3d we're attempting to place down
        private IMesh<? extends Object>.IMeshEdge edge3d; // edge in 3d that connects to this face (or null if it's the first item)
        private TEdge edge2d;  // edge in 2d that connects to the face (this is the translation from edge3d to the 2d plain)
        private Object otherVertexId; // (3d) id of the vertex that is NOT a part of the edge
        public HeapElem(Object faceId) {
            this.faceId = faceId;
            edge2d = null;
            edge3d = null;
            otherVertexId = null;
        }
        public HeapElem(Object faceId, IMesh<? extends Object>.IMeshEdge edge3d, TEdge edge2d) {
            this.faceId = faceId;
            this.edge3d = edge3d;
            this.edge2d = edge2d;
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
	        return faceMapping.get(getFaceId()).iFace;
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
    // private BitSet facesPlaced; // represents faces we have already placed down

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
                if(faceMapping.get(currentFaceId).isFacePlacedOnTexture())
                    continue;

                IMesh.IMeshEdge adjacentEdge3d = elem.edge3d;
                TEdge adjacentEdge2d = elem.edge2d;

                Point2D insertedPoint = null; // non-null if this isn't the first inserted triangle
                Point2D p1, p2, p3; // three points we will place down (and test against existing items in the RTree

                if(adjacentEdge2d != null && adjacentEdge3d != null) {
                    // we are looking at an item on the vertex front of the graph that is not the first item and shares an edge
                    // with an existing triangle on the texture plane.

                    // we have an existing edge in both 2d and 3d that this one coincides with.
                    // todo - what if there are multiple edges in 3d that coincide with this face, and we can place them all down in a flat manner?
                    //  e.g. a flat plane of 4 triangles arranged in a "triforce" manner.
                    //  Might be possible if, when we are going to add a new face, we check the existing connecting triangles to see if they are placed yet.
                    //  If they are, then we will use the existing point2d and point2d index if it's "close enough" to one of the connecting edges when we place it down
                    //  Then we use the existing point2d and the existing int 2d texture id.

                } else {
                    // we are looking at the first item of a potential subgraph. we project the
                    // 3d face onto the 2d surface such that v1 is at (0,0), v2 is at (0, k) and v3 is at (u, v)
                    IMesh.IMeshFace faceToPlace = backingMesh.getFace(currentFaceId);

                    // X in orthonormal basis
                    IVertex3D v12 = faceToPlace.getVertex2().subtract(faceToPlace.getVertex1());
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

                    // first vertex is at (0,0), second is at (len12, 0), third is at (t3x, t3y)
                    // where t3x = (v1 - v3) . unit(X), t3y = (v1 - v3) . unit(Y)
                    double t3x, t3y;
                    {
                        // we reuse v12, as we don't need to use it any longer
                        IVertex3D.subtract(faceToPlace.getVertex1(), faceToPlace.getVertex3(), v12);
                        double v20x = v12.getX().doubleValue();
                        double v20y = v12.getY().doubleValue();
                        double v20z = v12.getZ().doubleValue();
                        t3x = u12x*v20x + u12y*v20y + u12z*v20z;
                        t3y = xC*v20x + yC*v20y + zC*v20z;
                    }

                    p1 = Point2D.ZERO;
                    p2 = new Point2D(len12, 0.0);
                    p3 = new Point2D(t3x, t3y);
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
                // todo - add face to list of points, obtain indices
                //      this is the part where we search other adjacent faces, see if they are already on the texture map, and get the relevant coordinates if so.
                TFace newFace = new TFace();
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

                    // current: A, B, C
                    // other: D, E, F
                    // (B - A) x (C - B) = normalCurrent
                    // (E - D) x (F - E) = normalOther
                    // normalCurrent . normalOther = ||normalCurrent|| * ||normalOther|| cos(theta)
                    double ranking = getRanking(currentFace, otherFace);

                    // translate 3d edge to its 2d equivalent. Since we're guaranteed that currentFaceId
                    // already is mapped to 2d texture, all vertices should have entries.

                    HeapElem newElem = new HeapElem(otherFaceId);
                    newElem.edge3d = edge;
                    setCommonEdge_ref(currentFace, otherFace, newFace, newElem);
                    // populate HeapElem such that we map edge3d to the appropriate edge2d, following winding orders

                    // I don't think we need to worry about edges being flipped, same vertex1 in 3d maps to same vertex1 in 2d (likewise for vertex2)
                    // todo - check logic when we place the face down in 2d. If we do it correctly wrt winding order
                    //        we won't have to do it here.

                    HeapOrder newElemOrder = new HeapOrder(ranking, otherFaceId, newElem.edge2d);

                    heap.insert(newElemOrder, newElem);
                }
            }
        }

        /**
         perform BFS: add element 0 to the heap
         
         take an element off the heap
         if element edge is null, then we...
         project face to texture 
         add face to texture vertices
         add face to texture faces
         add face to mapping
         go to all neighboring edges, calculate theta, add in tEdge if it exists, add to heap.
         
         if element edge is not null
         we already have location for two t vertices. Find the location for the third.
         rtree intersection test to see if it can be placed
         if it can, update mapping to list id as being placed. Add tface to rtree. update max and min bounds
         if it cannot, then continue
         
         if heap is empty then we check tonsee if there are any leftover items in mapping, ones without texture face placed. Start process again
         
         collect face coordinates for each along with bounding boxes
         */
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

    private void addFace(TFace face, int vert1, int vert2) {

    }
    private void addFace(TFace face) {
//        intersectionTest.add(face, );
        int insertIndex = tFaces.size();
        tFaces.add(face);
    }
}

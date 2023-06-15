package io.hostilerobot.ceramicrelief.texture;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.SearchRTree;
import com.github.davidmoten.rtree2.geometry.Geometry;
import com.github.davidmoten.rtree2.geometry.internal.RectangleDouble;
import io.hostilerobot.ceramicrelief.imesh.IMesh;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.shape.TriangleMesh;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.math.util.FastMath;
import org.apache.commons.math.util.MathUtils;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jgrapht.traverse.ClosestFirstIterator;
import org.jheaps.AddressableHeap;
import static org.jheaps.AddressableHeap.Handle;
import org.jheaps.tree.PairingHeap;

import java.util.*;
import java.util.function.Predicate;


// rather than managing a bunch of stuff for treesets and keeping the bounding boxes this way,
// we can just use a RTree library. Then I don't have to fix as many bugs
// essentially represents a texture that
public class BoundaryTexture {
    private IMesh<? extends Object> backingMesh;
        // since we're not adding items we really don't care about the type of the backing ID, however this is still useful in lookups

    private static class TEdge {
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
    // our list of faces on the texture. Faces indexed to tVertices
    private List<TFace> tFaces;
    private RTree<TFace, Triangle2D> intersectionTest;
    private Map<Object, FaceInfo> mapping; // mapping of 3d object ID to index of 2d representation and internal index for 3d representation

    private void fillMapping() {
        Set<? extends Object> ids = backingMesh.getFaces();
        if(ids.stream().allMatch(x -> x instanceof Integer)) {
            // just use the original mapping. Keep the user's ordering
            for(Object id : ids) {
                mapping.put(id, new FaceInfo((int)id));
            }
        } else {
            // otherwise user could be using strings and hand-constructing the shape. Use the order they were inserted in
            int iFacePos = 0;
            for(Object id : backingMesh.getFaces()) {
                mapping.put(id, new FaceInfo(iFacePos++));
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
        this.intersectionTest = RTree.create();
        this.backingMesh = backingMesh;
        this.mapping = new HashMap<>();

        // make an integer mapping for the faces in the mesh from Object ID -> int
        // Object ID is already the type of int, then we keep the original mapping.
        // however,
        fillMapping();
    }

    private static class HeapElem{
        private Object faceId; // id of the face in 3d we're attempting to place down
        private IMesh<? extends Object>.IMeshEdge edge3d; // edge in 3d that connects to this face (or null if it's the first item)
        private TEdge edge2d;  // edge in 2d that connects to the face (this is the translation from edge3d to the 2d plain)
        public HeapElem(Object faceId) {
            this.faceId = faceId;
            edge2d = null;
            edge3d = null;
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
	        return mapping.get(getFaceId()).iFace;
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
            if(mapping.get(id).isFacePlacedOnTexture())
                continue;

            // add an initial element
            HeapOrder initialOrder = new HeapOrder(id);
            HeapElem initialElem = new HeapElem(id);
            heap.insert(initialOrder, initialElem);


            while(!heap.isEmpty()) {
                // we already placed this item.
                // note we may have duplicates of the 3d face in the heap if we add it along different connecting edges
                if(mapping.get(id).isFacePlacedOnTexture())
                    continue;

                // get an item off the heap
                Handle<HeapOrder, HeapElem> item = heap.deleteMin();
                HeapElem elem = item.getValue();
                Object currentFaceId = elem.faceId;
                IMesh.IMeshEdge adjacentEdge3d = elem.edge3d;
                TEdge adjacentEdge2d = elem.edge2d;

//                Point2D insertedPoint = null; // non-null if this isn't the first inserted triangle
//                int v1, v2, v3; // indices of the vertices

                // attempt to place this face onto the 2d texture
                if(adjacentEdge2d == null && adjacentEdge3d == null) {
                    // first face to be placed down.
                    // place the face down edge so that vertices are at [0,0], [0, k], and [u, v]
                } else {
                    // face to be placed down, such that it's adjacent to adjacentEdge2d
                }

                Triangle2D newTriangle = null;
                Iterable<Entry<TFace, Triangle2D>> entries = SearchRTree.search(intersectionTest, newTriangle);
                if(entries.iterator().hasNext()) {
                    // there is an intersection with the new triangle we're attempting to place down
                    // (we don't exactly care with what triangle)
                    // therefore we don't try to place down this triangle as it would overlap, and we continue with the next item in the heap
                    continue;
                } else {
                    // otherwise place this face down.
                    // add the new 2d vertex to our list and link it up with newFace
                    TFace newFace = new TFace();
                    intersectionTest = intersectionTest.add(newFace, newTriangle);
                }

                // run through the edges that connect to the current one
                for(IMesh<Object>.IMeshEdge edge : connectivity.iterables().edgesOf(currentFaceId)) {
                    // id of the other face
                    Object otherFaceId = Graphs.getOppositeVertex(connectivity, edge, currentFaceId);
                    IMesh.IMeshFace currentFace = backingMesh.getFace(currentFaceId);
                    IMesh.IMeshFace otherFace = backingMesh.getFace(otherFaceId);

                    // current: A, B, C
                    // other: D, E, F
                    // (B - A) x (C - B) = normalCurrent
                    // (E - D) x (F - E) = normalOther
                    // normalCurrent . normalOther = ||normalCurrent|| * ||normalOther|| cos(theta)

                    //
//                    FastMath.atan2()
//                    currentFace.getVertex1()
//                            .cross(currentFace.getVertex2());
//                    otherFace.getVertex1().
//                            .cross(currentFace.getVertex2());

                }
//                List<Object> neighbors = ((IMesh<Object>)backingMesh).iterables()getFace(currentFace).iterables();



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
        // we can do some math tricks to minimize the number of operations.
        double dotProd = 0.0;
        double tan = 0.0;
        double cot = 0.0;

        double ranking1 = -FastMath.abs(tan);
        double ranking2 = -FastMath.abs(cot);
        double result;
        if(ranking1 < ranking2) {
            result = -ranking2; // Domain: (pi/4 to pi/2] and (-pi/4 to -pi/2]. Range: 1 to 0
        } else {
            result = ranking1 + 2.0; // Domain: [-pi/4 to pi/4]. Range: (2 to 1)
        }

        if(dotProd < 0) {
            // Domain: (pi/2 to pi) and (-pi/2 to -pi). Range (0 to -2]
            result = -2.0 - result;
        }
        // function is a monotone, decreasing on the domain [0 to pi] and [-pi to 0]
        // range goes from [2 to -2]
        // such that value decreases as we get further from zero till pi or -pi, and begin increasing back again
        return result;
    }

    private void addFace(TFace face, int vert1, int vert2) {

    }
    private void addFace(TFace face) {
//        intersectionTest.add(face, );
        int insertIndex = tFaces.size();
        tFaces.add(face);
    }
}

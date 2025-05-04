package io.hostilerobot.ceramicrelief.texture.mesh_traversal;

import com.github.davidmoten.rtree2.Entry;
import com.github.davidmoten.rtree2.RTree;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.intersection.SearchRTree;
import com.github.davidmoten.rtree2.geometry.internal.GeometryUtil;
import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.qmesh.QMeshFace;
import io.hostilerobot.ceramicrelief.qmesh.QVertex3D;
import io.hostilerobot.ceramicrelief.texture.mesh_traversal.intersection.Triangle2D;
import io.hostilerobot.ceramicrelief.texture.projection.FaceMappingInfo;
import io.hostilerobot.ceramicrelief.texture.projection.ProjectionState;
import io.hostilerobot.ceramicrelief.texture.TEdge;
import io.hostilerobot.ceramicrelief.texture.TEdgeConnectionPolicy;
import io.hostilerobot.ceramicrelief.texture.TFace;
import io.hostilerobot.ceramicrelief.util.Epsilon;
import javafx.geometry.Point2D;
import org.apache.commons.math.util.FastMath;
import org.jgrapht.Graph;
import org.jgrapht.Graphs;
import org.jheaps.AddressableHeap;
import org.jheaps.tree.PairingHeap;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 *
 * projects a partition of a 3d mesh to 2d space. Does this by BFS traversal from each face.
 * Creates a series of contiguous polygons comprised of 2d triangles. Each polygon
 * represent a disjoint subset of the faces on the 3d mesh, such that all faces are placed.
 *
 * the traversal starts from an individual face in the IMesh, and places down as many adjacent faces as possible
 *
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
public class DFSMeshPartitionTraversal implements MeshPartitionTraversal {

    private static void populateEdge1_2(QMesh mesh, QMeshFace faceToAdd, HeapElem reference) {
        QVertex3D v2 = mesh.getVertex(faceToAdd.getV2());
        // we create these vector in the same direction that they are defined
        // such that they are increasing e.g. 1->2, 2->3, or 3->1
        // triVector goes from the end of the edgeVector to the new point
        reference.edgeVector = v2.subtract(mesh.getVertex(faceToAdd.getV1()));
        reference.triVector = mesh.getVertex(faceToAdd.getV3()).subtract(v2);
        reference.vertexToPlace = faceToAdd.getV3();
    }

    private static void populateEdge2_3(QMesh mesh, QMeshFace faceToAdd, HeapElem reference) {
        QVertex3D v3 = mesh.getVertex(faceToAdd.getV3());
        reference.edgeVector = v3.subtract(mesh.getVertex(faceToAdd.getV2()));
        reference.triVector = mesh.getVertex(faceToAdd.getV1()).subtract(v3);
        reference.vertexToPlace = faceToAdd.getV1();
    }

    private static void populateEdge3_1(QMesh mesh, QMeshFace faceToAdd, HeapElem reference) {
        QVertex3D v1 = mesh.getVertex(faceToAdd.getV1());
        reference.edgeVector = v1.subtract(mesh.getVertex(faceToAdd.getV3()));
        reference.triVector = mesh.getVertex(faceToAdd.getV2()).subtract(v1);
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

    private static class HeapElem{
        private boolean isWindingSame; // is the winding the same between the two edges?
        private int vertexToPlace;
        private int faceId; // id of the face in 3d we're attempting to place down
        private int oppositeTVertex; // tVertex on the opposite side from edge2d
        private QMeshEdge adjacentFace; // edge in 3d that connects to this face (or null if it's the first item)
        private TEdge edge2d;  // edge in 2d that connects to the face (this is the translation from edge3d to the 2d plain)
        private QVertex3D edgeVector; // vector that describes the adjacent edge we're laying down
        private QVertex3D triVector; // vector that describes the new triangle we're laying down
        private TEdgeConnectionPolicy connectionPolicy;
        public HeapElem(int faceId) {
            this.faceId = faceId;
            edge2d = null;
            adjacentFace = null;
        }
        public HeapElem(int faceId, QMeshEdge adjacentFace) {
            this.faceId = faceId;
            this.adjacentFace = adjacentFace;
        }
        public boolean canBeConnected() {
            return connectionPolicy.connected(isWindingSame);
        }
        public boolean placeAllVertices() {
            return edge2d == null;
        }
        public int getVertexToPlace() {
            return vertexToPlace;
        }
    }

    // as we traverse the 3d graph, we place down faces that are similar enough in angle to faces that are already down first
    // we break ties by choosing the lowest ID face to be placed
    // we break ties by choosing the lowest connecting edge in 2d
    private static class HeapOrder {
        // constructor should only be utilized when inserting the first item into the heap
        public HeapOrder(int faceOrder) {
            this.faceOrder = faceOrder;
            this.connectingEdge = null;
            this.theta = -1;
        }
        public HeapOrder(int faceOrder, TEdge connectingEdge, double theta) {
            // faceId, connectingEdge must not be null.
            this.faceOrder = faceOrder;
            this.connectingEdge = connectingEdge;
            this.theta = theta;
        }
        private int faceOrder;
        private double theta; // A . B = ||A|| ||B|| cos(theta)
        // A . B / ||A|| ||B|| = cos(theta)
        // dot product of two normals on two faces = cos(theta). 1 if they are parallel
        // 0 if they are perpendicular. -1 if they are in opposite direction.
        // therefore, for ordering purposes, we order by -cos(theta) since this is a min-ordered heap
        // used to break ties.
        private TEdge connectingEdge; // 2d edge in texture that connects to the face we're attempting to place down (null if there is none)
        public double getTheta() {
            return theta;
        }
        public int getFaceOrder() {
            return faceOrder;
        }

        public TEdge getConnectingEdge() {
            return connectingEdge;
        }
    }

    private static final Comparator<HeapOrder> HEAP_ORDER_COMPARATOR =  Comparator.comparingDouble(HeapOrder::getTheta)
            .thenComparingInt(HeapOrder::getFaceOrder)
            .thenComparing(HeapOrder::getConnectingEdge, Comparator.nullsFirst(Comparator.comparingInt(TEdge::getV1).thenComparing(TEdge::getV2)));

    /**
     * not-so-elegant solution for finding a common face between two edges and finding where the expected texture coordinates should be
     * (accounting for winding order, etc). Also calculates which vertex on the second face we will be placing down
     *
     * extracts the common edge of the two faces based on comparing the reference IDs of the vertices
     * then it translates it to the edge of the texture coordinates by extracting it from {@param firstFace}, which represents
     * the coordinates for the first face.
     */
    private static void prepareForPlacement(QMesh mesh, QMeshFace first, QMeshFace second, TFace firstFace,
                                            HeapElem reference) {
        // we only need to check currentFace.v1 and currentFace.v2
        // since they're sharing an edge, either one or the other will match
        if(first.getV1() == second.getV1()) {
            if(first.getV3() == second.getV2()) { // edge first[1--3] == second[1--2]
                populateTEdge3_1Same(firstFace, reference);
                populateEdge1_2(mesh, second, reference);
            } else if(first.getV2() == second.getV3()) { // edge first[1--2] == second[1--3]
                populateTEdge1_2Same(firstFace, reference);
                populateEdge3_1(mesh, second, reference);
            } // opposite windings
            else if(first.getV2() == second.getV2()) { // edge first[1--2] == second[1--2]
                populateTEdge1_2Diff(firstFace, reference);
                populateEdge1_2(mesh, second, reference);
            } else if(first.getV3() == second.getV3()) { // edge first[1--3] == second[1--3]
                populateTEdge3_1Diff(firstFace, reference);
                populateEdge3_1(mesh, second, reference);
            }
        } else if(first.getV1() == second.getV2()) {
            if(first.getV2() == second.getV1()) { // edge first[1--2] == second[2--1]
                populateTEdge1_2Same(firstFace, reference);
                populateEdge1_2(mesh, second, reference);
            } else if(first.getV3() == second.getV3()) { // edge first[1--3] == second[2--3]
                populateTEdge3_1Same(firstFace, reference);
                populateEdge2_3(mesh, second, reference);
            } // different winding conditions
            else if(first.getV2() == second.getV3()) { // edge first[1--2] == second[2--3]
                populateTEdge1_2Diff(firstFace, reference);
                populateEdge2_3(mesh, second, reference);
            } else if(first.getV3() == second.getV1()) { // edge first[1--3] == second[2--1]
                populateTEdge3_1Diff(firstFace, reference);
                populateEdge1_2(mesh, second, reference);
            }
        } else if(first.getV1() == second.getV3()) {
            if(first.getV2() == second.getV2()) { // edge first[1--2] == second[3--2]
                populateTEdge1_2Same(firstFace, reference);
                populateEdge2_3(mesh, second, reference);
            } else if(first.getV3() == second.getV1()) { // edge first[1--3] == second[3--1]
                populateTEdge3_1Same(firstFace, reference);
                populateEdge3_1(mesh, second, reference);
            } // opposite windings
            else if(first.getV2() == second.getV1()) { // edge first[1--2] == second[3--1]
                populateTEdge1_2Diff(firstFace, reference);
                populateEdge3_1(mesh, second, reference);
            } else if(first.getV3() == second.getV2()) { // edge first[1--3] == second[3--2]
                populateTEdge3_1Diff(firstFace, reference);
                populateEdge2_3(mesh, second, reference);
            }
        } else if(first.getV2() == second.getV1()) {
            if(first.getV3() == second.getV3()) { // edge first[2--3] == second[1--3]
                populateTEdge2_3Same(firstFace, reference);
                populateEdge3_1(mesh, second, reference);
            } // opposite winding
            else if(first.getV3() == second.getV2()) { // edge first[2--3] == second[1--2]
                populateTEdge2_3Diff(firstFace, reference);
                populateEdge1_2(mesh, second, reference);
            }
        } else if(first.getV2() == second.getV2()) {
            if(first.getV3() == second.getV1()) { // edge first[2--3] == second[2--1]
                populateTEdge2_3Same(firstFace, reference);
                populateEdge1_2(mesh, second, reference);
            } // opposite winding
            else if(first.getV3() == second.getV3()) { // edge first[2--3] == second[2--3]
                populateTEdge2_3Diff(firstFace, reference);
                populateEdge2_3(mesh, second, reference);
            }
        } else if(first.getV2() == second.getV3()) {
            if(first.getV3() == second.getV2()) { // edge first[2--3] == second[3--2]
                populateTEdge2_3Same(firstFace, reference);
                populateEdge2_3(mesh, second, reference);
            } // opposite winding
            else if(first.getV3() == second.getV1()) { // edge first[2--3] == second[3--1]
                populateTEdge2_3Diff(firstFace, reference);
                populateEdge3_1(mesh, second, reference);
            }
        }
        // these should be set by the end
        assert reference.edge2d != null && reference.edgeVector != null && reference.triVector != null;
    }

    private static void prepareConnectionPolicy(
            HeapElem reference,
            Map<QMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy) {
        // populate policy: set it to default if it's not already defined
        reference.connectionPolicy = edgeConnectionPolicy.computeIfAbsent(reference.adjacentFace,
                k -> TEdgeConnectionPolicy.getDefaultPolicy(reference.isWindingSame));
        assert reference.connectionPolicy != null; // policy should be set too.
    }


    @Override
    public ProjectedTextureInfo projectSubset(int initialFace, ProjectionState projectionState) {
        return traverse(initialFace, projectionState.getMesh(),
                projectionState.getProjection().getTFaces(),
                projectionState.getProjection().getTVertices(),
                projectionState.getProjection().getFaceMapping(),
                projectionState.getConnections());
    }

    private ProjectedTextureInfo traverse(int initialMeshFace, QMesh backingMesh,
                                          List<TFace> tFaces, List<Point2D> tVertices,
                                          FaceMappingInfo faceMapping,
                                          Map<QMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy) {

        ProjectedTextureInfo result = new ProjectedTextureInfo();
        // way to test for intersection among 3d triangles in a 2d plane
        // start with an empty intersection test each time we begin traversing the mesh.
        RTree<TFace, Triangle2D> intersectionTest = RTree.create();
        PairingHeap<HeapOrder, HeapElem> heap = new PairingHeap<>(HEAP_ORDER_COMPARATOR);
        Graph<Integer, QMeshEdge> connectivity = backingMesh.getMeshConnectivity();

        // add an initial element
        HeapOrder initialOrder = new HeapOrder(initialMeshFace);
        HeapElem initialElem = new HeapElem(initialMeshFace);
        heap.insert(initialOrder, initialElem);

        while(!heap.isEmpty()) {
            // get an item off the heap
            AddressableHeap.Handle<HeapOrder, HeapElem> item = heap.deleteMin();
            HeapElem elem = item.getValue();
            int currentMeshFaceId = elem.faceId;

            // we already placed this item from the heap.
            // note we may have duplicates of the 3d face in the heap if we add it along different connecting edges
            if(faceMapping.isFacePlacedOnTexture(currentMeshFaceId)) {
                continue;
            }

            QMeshFace faceToPlace = backingMesh.getFace(currentMeshFaceId);

            TEdge adjacentEdge2d = elem.edge2d;

            Point2D insertedPoint = null; // non-null if this isn't the first inserted triangle
            Point2D p1, p2, p3; // three points we will place down (and test against existing items in the RTree

            // project the triangle onto the 2d plane, preserving the length of the sides
            // calculation is mostly the same between placing the first triangle and placing subsequent triangles
            {
                QVertex3D vertex1 = null;
                QVertex3D v12;
                if(!elem.placeAllVertices()) {
                    assert faceMapping.isFacePlacedOnTexture(elem.adjacentFace.getOtherFace(currentMeshFaceId));
                    // the existing face must already be placed on the texture.

                    assert elem.canBeConnected(); // we should only add edges to the heap that can be connected
                    // this lines up with p1 -> p2
                    v12 = elem.edgeVector;
                } else {
                    vertex1 = backingMesh.getVertex(faceToPlace.getV1());
                    QVertex3D vertex2 = backingMesh.getVertex(faceToPlace.getV2());
                    v12 = vertex2.subtract(vertex1);
                }

                double len12 = v12.length();

                // X in unit vector
                double u12x = v12.getX() / len12;
                double u12y = v12.getY() / len12;
                double u12z = v12.getZ() / len12;

                // Z in orthonormal basis
                QVertex3D normal = backingMesh.getNormal(currentMeshFaceId);
                double lenNormal = normal.length();

                // Z in unit vector
                double uNormalx = normal.getX() / lenNormal;
                double uNormaly = normal.getY() / lenNormal;
                double uNormalz = normal.getZ() / lenNormal;

                // Y in orthonormal basis, where Y = X x Z
                // this is already a unit vector, as X and Z are units.
                double xC = (u12y*uNormalz) - (u12z*uNormaly);
                double yC = (u12z*uNormalx) - (u12x*uNormalz);
                double zC = (u12x*uNormaly) - (u12y*uNormalx);

                QVertex3D v31;
                if(!elem.placeAllVertices()) {
                    v31 = elem.triVector;
                } else {
                    // we can reuse v12, as we don't need to use it any longer
                    QVertex3D vertex3 = backingMesh.getVertex(faceToPlace.getV3());
                    QVertex3D.subtract(vertex1, vertex3, v12);
                    v31 = v12;
                }

                // first vertex is at (0,0), second is at (len12, 0), third is at (t3x, t3y)
                // where t3x = (v1 - v3) . unit(X), t3y = (v1 - v3) . unit(Y)
                double t3x, t3y;
                {
                    double v31x = v31.getX();
                    double v31y = v31.getY();
                    double v31z = v31.getZ();
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

                    insertedPoint = p3;

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

            Triangle2D newTriangle = new Triangle2D(//insertedPoint,
                    p1, p2, p3);

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
            TFace newFace;
            if(!elem.placeAllVertices()) {
                Integer commonTVertex = null;
                double currentMinDistance = 0; // set this for java compiler. However this is guaranteed to be set before it is read from.
                for(QMeshEdge outgoing : connectivity.iterables().edgesOf(currentMeshFaceId)) {
                    int otherMeshFaceId = Graphs.getOppositeVertex(connectivity, outgoing, currentMeshFaceId);
                    // graph connection from otherFaceId to currentFaceId
                    if(faceMapping.isFacePlacedOnTexture(otherMeshFaceId)) {
                        TFace adjacentTexture = tFaces.get(faceMapping.getTFace(otherMeshFaceId));

                        // if the neighboring face is already placed on the texture, we may can consolidate the
                        // new point to an existing one if they're very close together
                        QMeshFace adjacentFace = backingMesh.getFace(otherMeshFaceId);
                        // find vertex in common with insertedPoint

                        int adjacentTIndex;
                        if(adjacentFace.getV1() == elem.getVertexToPlace()) {
                            adjacentTIndex = adjacentTexture.getV1();
                        } else if(adjacentFace.getV2() == elem.getVertexToPlace()) {
                            adjacentTIndex = adjacentTexture.getV2();
                        } else if(adjacentFace.getV3() == elem.getVertexToPlace()) {
                            adjacentTIndex = adjacentTexture.getV3();
                        } else{
                            continue;
                        }

                        Point2D adjacentTVertex = tVertices.get(adjacentTIndex);
                        double currentDistance = GeometryUtil.distanceSquared(adjacentTVertex.getX(), adjacentTVertex.getY(),
                                insertedPoint.getX(), insertedPoint.getY());
                        // ensure that the common vertex that we find is actually close to the new 2d vertex we're attempting to lay down
                        if((commonTVertex == null && currentDistance <= Epsilon.epsilonSq())
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
                    result.addPoint(insertedPoint);
                } else {
                    // we found a vertex that is already on the 2d texture map
                    // so we have it map to an existing item in our array.
                    newIndex = commonTVertex;
                }

                // we ensure that v1, v2, and v3 are defined in the original order as in IMesh.
                if(elem.getVertexToPlace() == faceToPlace.getV1()) {
                    newFace = new TFace(newIndex,
                            adjacentEdge2d.getV1(),
                            adjacentEdge2d.getV2());
                } else if(elem.getVertexToPlace() == faceToPlace.getV2()) {
                    newFace = new TFace(adjacentEdge2d.getV2(),
                            newIndex,
                            adjacentEdge2d.getV1());
                } else if(elem.getVertexToPlace() == faceToPlace.getV3()) {
                    newFace = new TFace(adjacentEdge2d.getV1(),
                            adjacentEdge2d.getV2(),
                            newIndex);
                } else {
                    throw new IllegalStateException(elem.getVertexToPlace() + " not in " + faceToPlace);
                }

            } else {
                // we are placing the first triangle in a mesh subset
                // we add all three vertices to the data structure, then add the face
                int tri1 = tVertices.size();
                newFace = new TFace(tri1,
                        tri1 + 1,
                        tri1 + 2);

                tVertices.add(p1);
                tVertices.add(p2);
                tVertices.add(p3);
                result.addPoints(p1, p2, p3);
            }

            // add the new face to our list of faces
            int newTFaceId = tFaces.size();
            faceMapping.setTFace(currentMeshFaceId, newTFaceId); // update our mapping
            tFaces.add(newFace);
            result.incrementFaceCount();
            intersectionTest = intersectionTest.add(newFace, newTriangle);

            // run through the edges that connect to the current one
            for(QMeshEdge edge : connectivity.iterables().edgesOf(currentMeshFaceId)) {
                // id of the other face
                int otherMeshFaceId = Graphs.getOppositeVertex(connectivity, edge, currentMeshFaceId);
                // this face is already placed on the texture. We don't process it.
                if(faceMapping.isFacePlacedOnTexture(otherMeshFaceId))
                    continue;
                QMeshFace currentFace = backingMesh.getFace(currentMeshFaceId);
                QMeshFace otherFace = backingMesh.getFace(otherMeshFaceId);

                // traverse to the new face (eventually) by putting it onto the heap
                HeapElem newElem = new HeapElem(otherMeshFaceId, edge);
                prepareForPlacement(backingMesh, currentFace, otherFace, newFace, newElem); // populate information for the HeapElem so it can be placed on the 2d texture
                prepareConnectionPolicy(newElem, edgeConnectionPolicy);

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
                double ranking = getRanking(backingMesh, currentMeshFaceId, otherMeshFaceId);
                HeapOrder newElemOrder = new HeapOrder(otherMeshFaceId,
                        newElem.edge2d, ranking);

                heap.insert(newElemOrder, newElem);
            }
        }
        return result;
    }

    private static double getRanking(QMesh mesh, int currentFace, int otherFace) {
        // simpler method: return -cos(theta), which is calculated from the dot product of the two normals
        QVertex3D n1 = mesh.getNormal(currentFace);
        QVertex3D n2 = mesh.getNormal(otherFace);
        double dotProd = n1.dot(n2);
        double vectorLength = Math.sqrt(n1.dot(n1) * n2.dot(n2));
        // return -cosTheta. This has the desired effect of being the highest value at theta PI and -PI, and lowest value at theta 0
        return -(dotProd / vectorLength);
    }
}

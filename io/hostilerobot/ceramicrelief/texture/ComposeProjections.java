package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import javafx.geometry.Point2D;
import org.jgrapht.Graph;
import org.jgrapht.graph.SimpleGraph;

import java.util.List;
import java.util.Map;

/**
 *
 * todo: refactor such that data is its own structure we can pass around, then we have an implementation function
 *       that transforms the data. Currently, doing all the work in the constructor is a dumb design pattern
 */
public class ComposeProjections {
    private Graph<TFace, EdgeInfo> textureConnections; // describes the relationship between triangles in our 2d texture
    private List<Point2D> packing;
    private ProjectMesh projection;

    public ComposeProjections(
            List<Point2D> packing,
            ProjectMesh projection) {
        assert packing != null && projection != null
                && packing.size() == projection.getTraversals().size();
        this.textureConnections = new SimpleGraph<>(null, null, false);
        this.packing = packing;
        this.projection = projection;

        translateProjections();
        createTGraph();
    }
    private void translateProjections() {
        // based on the packing (which defines the new top-left corner for each bounding box in the traversal)
        // compose into one texture.
        // things we want to have as a result:
        // new list of vertices, new list of points
        // graph (V, E) describing the relation between TFaces
        int currentVertex = 0;
        List<MeshProjectionTraversal> traversals = projection.getTraversals();
        for(int idx = 0; idx < traversals.size(); idx++) {
            MeshProjectionTraversal traversal = traversals.get(idx);
            Point2D topLeft = packing.get(idx);

            double translateX = traversal.getBounds().getMinX() - topLeft.getX();
            double translateY = traversal.getBounds().getMinY() - topLeft.getY();

            int vertexCount = traversal.getTVertexCount();
            int vertexEnd = currentVertex + vertexCount;
            List<Point2D> vertices = projection.getTVertices();
            for(int vert = currentVertex; vert < vertexEnd; vert++) {
                Point2D vertex = vertices.get(vert);
                Point2D translated = vertex.add(translateX, translateY);
                vertices.set(vert, translated);
            }

            currentVertex = vertexEnd;
        }
    }

    // todo move to its own class
    private void createTGraph() {
        Map<QMesh.QMeshEdge, TEdgeConnectionPolicy> connections = projection.getEdgeConnectionPolicy();
        FaceMappingInfo faceMapping = projection.getFaceMapping(); // map mesh face to tface.
        Graph<Integer, QMesh.QMeshEdge> graph = projection.getBackingMesh().getMeshConnectivity();

        // note: suppose we have a face in 3d (A, B, C) that is projected onto 2d (tA, tB, tC)
        // then A -> tA, B -> tB, C -> tC from our traversal, since we ensure the order.
        for(int meshFaceSourceId : graph.iterables().vertices()) {
            int tFaceSourceId = faceMapping.getTFace(meshFaceSourceId);
            TFace tFaceSource = projection.getTFaces().get(tFaceSourceId);
            textureConnections.addVertex(tFaceSource); // ensure this vertex exists

            QMesh.QMeshFace meshFaceSource = projection.getBackingMesh().getFace(meshFaceSourceId);
            for(QMesh.QMeshEdge meshEdge : graph.iterables().edgesOf(meshFaceSourceId)) {
                int meshFaceDestId = meshEdge.getOtherFace(meshFaceSourceId);
                // run through adjacent faces
                int tFaceDestId = faceMapping.getTFace(meshFaceDestId);
                TFace tFaceDest = projection.getTFaces().get(tFaceDestId);
                if(textureConnections.containsEdge(tFaceSource, tFaceDest))
                    // if the edge already exists in the graph no need to process it again
                    continue;

                textureConnections.addVertex(tFaceDest); // ensure the target vertex exists in the graph

                QMesh.QMeshFace meshFaceDest = projection.getBackingMesh().getFace(meshFaceDestId);

                // retrieve the two edges from the texture
                TEdge sourceEdge = null;
                TEdge destEdge = null;
                {
                    // 18 total cases. 3 possible orientations of first triangle, 6 possible orientations of second triangle (3 rotations x reflection)
                    // OR 9 possible combinations to match first vertex, 2 possible combinations to match second vertex
                    if(meshFaceSource.getV1() == meshFaceDest.getV1()) {
                        if(meshFaceSource.getV3() == meshFaceDest.getV2()) { // edge first[1--3] == second[1--2]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV1(), tFaceDest.getV2());
                        } else if(meshFaceSource.getV2() == meshFaceDest.getV3()) { // edge first[1--2] == second[1--3]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV2());
                            destEdge = new TEdge(tFaceDest.getV1(), tFaceDest.getV3());
                        } else if(meshFaceSource.getV2() == meshFaceDest.getV2()) { // edge first[1--2] == second[1--2]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV2());
                            destEdge = new TEdge(tFaceDest.getV1(), tFaceDest.getV2());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV3()) { // edge first[1--3] == second[1--3]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV1(), tFaceDest.getV3());
                        }
                    } else if(meshFaceSource.getV1() == meshFaceDest.getV2()) {
                        if(meshFaceSource.getV2() == meshFaceDest.getV1()) { // edge first[1--2] == second[2--1]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV2());
                            destEdge = new TEdge(tFaceDest.getV2(), tFaceDest.getV1());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV3()) { // edge first[1--3] == second[2--3]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV2(), tFaceDest.getV3());
                        } else if(meshFaceSource.getV2() == meshFaceDest.getV3()) { // edge first[1--2] == second[2--3]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV2());
                            destEdge = new TEdge(tFaceDest.getV2(), tFaceDest.getV3());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV1()) { // edge first[1--3] == second[2--1]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV2(), tFaceDest.getV1());
                        }
                    } else if(meshFaceSource.getV1() == meshFaceDest.getV3()) {
                        if(meshFaceSource.getV2() == meshFaceDest.getV2()) { // edge first[1--2] == second[3--2]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV2());
                            destEdge = new TEdge(tFaceDest.getV3(), tFaceDest.getV2());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV1()) { // edge first[1--3] == second[3--1]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV3(), tFaceDest.getV1());
                        } else if(meshFaceSource.getV2() == meshFaceDest.getV1()) { // edge first[1--2] == second[3--1]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV2());
                            destEdge = new TEdge(tFaceDest.getV3(), tFaceDest.getV1());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV2()) { // edge first[1--3] == second[3--2]
                            sourceEdge = new TEdge(tFaceSource.getV1(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV3(), tFaceDest.getV2());
                        }
                    } else if(meshFaceSource.getV2() == meshFaceDest.getV1()) {
                        if(meshFaceSource.getV3() == meshFaceDest.getV3()) { // edge first[2--3] == second[1--3]
                            sourceEdge = new TEdge(tFaceSource.getV2(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV1(), tFaceDest.getV3());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV2()) { // edge first[2--3] == second[1--2]
                            sourceEdge = new TEdge(tFaceSource.getV2(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV1(), tFaceDest.getV2());
                        }
                    } else if(meshFaceSource.getV2() == meshFaceDest.getV2()) {
                        if(meshFaceSource.getV3() == meshFaceDest.getV1()) { // edge first[2--3] == second[2--1]
                            sourceEdge = new TEdge(tFaceSource.getV2(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV2(), tFaceDest.getV1());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV3()) { // edge first[2--3] == second[2--3]
                            sourceEdge = new TEdge(tFaceSource.getV2(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV2(), tFaceDest.getV3());
                        }
                    } else if(meshFaceSource.getV2() == meshFaceDest.getV3()) {
                        if(meshFaceSource.getV3() == meshFaceDest.getV2()) { // edge first[2--3] == second[3--2]
                            sourceEdge = new TEdge(tFaceSource.getV2(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV3(), tFaceDest.getV2());
                        } else if(meshFaceSource.getV3() == meshFaceDest.getV1()) { // edge first[2--3] == second[3--1]
                            sourceEdge = new TEdge(tFaceSource.getV2(), tFaceSource.getV3());
                            destEdge = new TEdge(tFaceDest.getV3(), tFaceDest.getV1());
                        }
                    }
                }

                // these two should be set as we cover each case and the two mesh faces should be adjacent
                assert sourceEdge != null && destEdge != null;

                // get the policy we will use
                TEdgeConnectionPolicy connectionPolicy = connections.get(meshEdge);
                // add the edge to the texture graph
                EdgeInfo edge = new EdgeInfo(sourceEdge, destEdge, connectionPolicy, meshEdge);
                textureConnections.addEdge(tFaceSource, tFaceDest, edge);
            }
        }
    }

}

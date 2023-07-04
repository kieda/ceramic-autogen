package io.hostilerobot.ceramicrelief.texture.post_processing;

import io.hostilerobot.ceramicrelief.qmesh.QMesh;
import io.hostilerobot.ceramicrelief.qmesh.QMeshEdge;
import io.hostilerobot.ceramicrelief.qmesh.QMeshFace;
import io.hostilerobot.ceramicrelief.texture.data_projection.EdgeInfo;
import io.hostilerobot.ceramicrelief.texture.data_projection.FaceMappingInfo;
import io.hostilerobot.ceramicrelief.texture.data_projection.ProjectionState;
import io.hostilerobot.ceramicrelief.texture.projection.MeshProjectionResult;
import io.hostilerobot.ceramicrelief.texture.data_tex.TEdge;
import io.hostilerobot.ceramicrelief.texture.data_tex.TEdgeConnectionPolicy;
import io.hostilerobot.ceramicrelief.texture.data_tex.TFace;
import org.jgrapht.Graph;

class GenerateTextureGraph {
    private GenerateTextureGraph(){}

    static void process(ProjectionState projectionState) {
        QMesh mesh = projectionState.getMesh();
        MeshProjectionResult projection = projectionState.getProjection();
        FaceMappingInfo faceMapping = projection.getFaceMapping(); // map mesh face to tface.
        Graph<Integer, QMeshEdge> graph = mesh.getMeshConnectivity();
        Graph<TFace, EdgeInfo> textureConnections = projection.getTextureConnections();
        // note: suppose we have a face in 3d (A, B, C) that is projected onto 2d (tA, tB, tC)
        // then A -> tA, B -> tB, C -> tC from our traversal, since we ensure the order.
        for(int meshFaceSourceId : graph.iterables().vertices()) {
            int tFaceSourceId = faceMapping.getTFace(meshFaceSourceId);
            TFace tFaceSource = projection.getTFaces().get(tFaceSourceId);
            textureConnections.addVertex(tFaceSource); // ensure this vertex exists

            QMeshFace meshFaceSource = mesh.getFace(meshFaceSourceId);
            for(QMeshEdge meshEdge : graph.iterables().edgesOf(meshFaceSourceId)) {
                int meshFaceDestId = meshEdge.getOtherFace(meshFaceSourceId);
                // run through adjacent faces
                int tFaceDestId = faceMapping.getTFace(meshFaceDestId);
                TFace tFaceDest = projection.getTFaces().get(tFaceDestId);
                if(textureConnections.containsEdge(tFaceSource, tFaceDest))
                    // if the edge already exists in the graph no need to process it again
                    continue;

                textureConnections.addVertex(tFaceDest); // ensure the target vertex exists in the graph

                QMeshFace meshFaceDest = mesh.getFace(meshFaceDestId);

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
                TEdgeConnectionPolicy connectionPolicy = projectionState.getConnections().get(meshEdge);
                // add the edge to the texture graph
                EdgeInfo edge = new EdgeInfo(sourceEdge, destEdge, connectionPolicy, meshEdge);
                textureConnections.addEdge(tFaceSource, tFaceDest, edge);
            }
        }
    }
}

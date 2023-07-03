package io.hostilerobot.ceramicrelief.texture;


/**
 * this package contains the pipeline necessary for translating a 3d mesh into a 2d mesh used for drawing a texture
 *
 * Overall class: MeshProjectionStrategy. Implementation: SegmentedMeshProjectionStrategy
 *    inputs:  Map<QMesh<Object>.QMeshEdge, TEdgeConnectionPolicy> initialPolicy
 *             QMesh<Object> backingMesh
 *    outputs: List<TFace> tFaces
 *             List<Point2D> tVertices
 *             Map<Object, Integer> faceMapping
 *             Graph<TFace, EdgeInfo> textureConnections
 *
 *
 * Currently SegmentedMeshProjectionStrategy is the only implementation and follows a pipeline
 * 
 * parts of the pipeline:
 *   Disjoint Mesh Traversal Collector
 *      -- traverses the faces of the mesh on all its disjoint components
 *
 *   Contiguous Mesh Traversal
 *      -- traverses the faces of the mesh from a starting point, reaching all faces that can be contiguously placed on the same texture
 *      -- generates a bounding box for each disjoint traversal
 *   Box Packing
 *      -- finds a way to pack the result of each bounding box so they are not overlapping
 *   Box Translation
 *      -- combines the results of individual mesh traversals and box packing to find the final positions of all vertices
 *   Texture Graph Generation
 *      -- creates a (V, E) graph connecting the triangle texture faces to faces that are adjacent in the mesh
 * 
 *
 */
package io.hostilerobot.ceramicrelief.texture;


/**
 * this package contains the pipeline necessary for translating a 3d mesh into a 2d mesh used for drawing a texture
 *
 * Overall class: MeshProjectionStrategy. Implementation: SegmentedMeshProjectionStrategy
 *    inputs:  Map<QMesh<Object>.QMeshEdge, TEdgeConnectionPolicy> initialPolicy
 *             QMesh<Object> backingMesh
 *    outputs: List<TFace> tFaces
 *             List<Point2D> tVertices
 *             FaceMappingInfo faceMapping
 *             Graph<TFace, EdgeInfo> textureConnections
 *
 *
 * Currently SegmentedMeshProjectionStrategy is the only implementation and follows a pipeline
 * 
 * parts of the pipeline:
 *   DisjointMeshProjector:
 *      -- traverses the faces of the mesh on all its disjoint components
 *         List<MeshProjectionTraversal> traversals;
 *         List<TFace> tFaces;
 *         List<Point2D> tVertices;
 *         FaceMappingInfo faceMapping;
 *
 *         Map<QMeshEdge, TEdgeConnectionPolicy> edgeConnectionPolicy;
 *
 *   Contiguous Mesh Traversal
 *      -- traverses the faces of the mesh from a starting point, reaching all faces that can be contiguously placed on the same texture
 *      -- generates a bounding box for each disjoint traversal
 *   Box Packing
 *      -- finds a way to pack the result of each bounding box so they are not overlapping
 *   ComposeProjectionUtil: Post processing
 *      TranslateDisjointProjections: Box Translation
 *          -- combines the results of individual mesh traversals and box packing to find the final positions of all vertices
 *      GenerateTextureGraph: 2d graph generation
 *          -- creates a (V, E) graph connecting the triangle texture faces to faces that are adjacent in the mesh
 * 
 *
 */
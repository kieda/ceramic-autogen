package io.hostilerobot.ceramicrelief.texture;


/**
 * this package contains the pipeline necessary for translating a 3d mesh into a 2d mesh used for drawing a texture
 *
 * simply put,
 *    Projection = Traversal + Packing + Post Processing
 *
 * Overall class: MeshProjectionStrategy. Implementation: DFSMeshProjectionStrategy
 *
 * DFSMeshProjectionStrategy follows a pipeline outlined by DisjointMeshProjectionStrategy.
 *
 * parts of the pipeline:
 *   PartitionedMeshTraversal:
 *      -- traverses the mesh over multiple partitions
 *      MeshPartitionTraversal: (default implementation: DFSMeshPartitionTraversal)
 *         -- gets individual partition of mesh, traversal and projection for items in the partition
 *   BoxPacker2D: (default implementation: SquareBoxPacker2D)
 *      -- finds a packing for multiple bounding boxes such that they do not overlap
 *   ProjectionCompositionUtil.translateProjections:
 *      -- combines the result of the partitioned mesh traversal with the result of box packing to get the final positions of all 2d vertices
 *   ProjectionCompositionUtil.generateTextureGraph:
 *      -- creates a (V, E) graph connecting the triangle texture faces to mesh-adjacent faces
 */
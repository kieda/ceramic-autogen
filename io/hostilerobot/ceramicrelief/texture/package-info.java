package io.hostilerobot.ceramicrelief.texture;


/**
 * this package contains the pipeline necessary for translating a 3d mesh into a 2d mesh used for drawing a texture
 *
 * parts of the pipeline:
 *   Disjoint Mesh Traversal Identifier
 *      -- traverses the faces of the mesh on all its disjoint components
 *   Contiguous Mesh Traversal
 *      -- traverses the faces of the mesh from a starting point, reaching all faces that can be contiguously placed on the same texture
 *
 *   Box Packing
 *
 *
 */
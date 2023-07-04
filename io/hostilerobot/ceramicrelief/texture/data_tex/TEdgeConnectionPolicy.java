package io.hostilerobot.ceramicrelief.texture.data_tex;

/**
 * represents the relationship between two faces in a texture
 */
public enum TEdgeConnectionPolicy {
    /**
     * represents that the edge is adjacent between two faces in the texture
     * such that a texture wraps directly in one direction to another
     *
     * same winding order:
     *      (3d)                                           (2d)
     *    +----+                                         +----+
     *    | A  | \         ===> adjacent to edge   ===>  | A  | \
     *    |    | B\             edge u, v                |    | B\
     *    +----+--->                                     +----+--->
     *
     * different winding order:
     *      (3d)                                             u2            u1
     *    +------+ u                                         +      +------+
     *    | A  / |         ===> adjacent to edge   ===>    / |      | A    |
     *    |   / B|              edge u, v                 / B|      |      |
     *    +--<---+ v                                     <---+      +------+
     *                                                       v2            v1
     *
     *
     * this relationship preserves shape, but reverses directionality with different winding orders
     * this relationship has the same directionality for the same winding order
     */
    ADJACENT_EDGE(true, false),

    /**
     * represents that the edge mirrors an opposing adjacent face
     * we might choose this option if there are two edges against each other with opposing winding order
     *
     * We will lay out the texture based on our winding order relationship in 3d like below:
     *
     * same winding order:
     *     (3d)                                                 (2d)
     *         u                                             u1        u2
     *    +----+                                        +----+         +
     *    | A  | \         ===> mirrored across   ===>  | A  |       / |
     *    |    | B\             edge u, v               |    |      / B|
     *    +----+--->                                    +----+     <---+
     *         v                                             v1        v2
     *
     * different winding order:
     *     (3d)                                                 u'
     *    +------+ u                                    +------+
     *    | A  / |         ===> mirrored across   ===>  | A    | \
     *    |   / B|              edge u, v               |      |B \
     *    +--<---+ v                                    +------+--->
     *                                                          v'
     *
     * this relationship does not preserve shape, and has the same directionality with different winding orders
     * this relationship does not have the same directionality with the same winding order
     */
    MIRRORED_EDGE(false, true),

    /**
     * there is no edge or relationship in the texture between the two faces
     * note that this can be utilized to have no texture relationship between two faces even if they share an edge in 3d
     * this creates a seam on the mesh.
     */
    NO_EDGE(false, false);
    TEdgeConnectionPolicy(boolean isConnectedOnSameWinding, boolean isConnectedOnDifferentWinding) {
        this.isConnectedOnSameWinding = isConnectedOnSameWinding;
        this.isConnectedOnDifferentWinding = isConnectedOnDifferentWinding;
    }
    private final boolean isConnectedOnSameWinding;
    private final boolean isConnectedOnDifferentWinding;

    public boolean connected(boolean sameWinding) {
        return (sameWinding && isConnectedOnSameWinding)
                || (!sameWinding && isConnectedOnDifferentWinding);
    }

    public static TEdgeConnectionPolicy getDefaultPolicy(boolean hasSameWinding) {
        return hasSameWinding ? ADJACENT_EDGE : MIRRORED_EDGE;
    }
}

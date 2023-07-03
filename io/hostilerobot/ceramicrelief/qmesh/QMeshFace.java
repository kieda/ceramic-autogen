package io.hostilerobot.ceramicrelief.qmesh;

public class QMeshFace {
    // id for the face in the mesh
    private final int face;
    // ids for vertices of the face
    private final int v1;
    private final int v2;
    private final int v3;

    QMeshFace(int face, int v1, int v2, int v3) {
        this.face = face; // this face's ID
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;
    }

    public int getV1() {
        return v1;
    }

    public int getV2() {
        return v2;
    }

    public int getV3() {
        return v3;
    }
}

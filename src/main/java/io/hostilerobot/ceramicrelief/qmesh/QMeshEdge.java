package io.hostilerobot.ceramicrelief.qmesh;

import io.hostilerobot.ceramicrelief.util.Hash;

public class QMeshEdge {
    // represents IDs for two adjacent edges in the mesh
    // this is more specific than CMeshEdge, as more than two faces in the mesh can share an edge.
    // However, we disallow two faces from sharing more than one edge
    private final int face1, face2;

    QMeshEdge(int face1, int face2) {
        this.face1 = face1;
        this.face2 = face2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QMeshEdge that = (QMeshEdge) o;

        // it's the same edge if we're switching up the order
        return (getFace1() == that.getFace2() && getFace2() == that.getFace2())
                || (getFace1() == that.getFace2() && getFace2() == that.getFace1());
    }

    public int getFace1() {
        return face1;
    }

    public int getFace2() {
        return face2;
    }

    public int getOtherFace(int face) {
        if (face == getFace1()) {
            return getFace2();
        } else {
            assert face == getFace2();
            return getFace1();
        }
    }

    @Override
    public int hashCode() {
        return Hash.hashSymmetric(getFace1(), getFace2());
    }

    public String toString() {
        return getFace1() + " -- " + getFace2();
    }
}

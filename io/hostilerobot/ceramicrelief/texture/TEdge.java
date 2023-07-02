package io.hostilerobot.ceramicrelief.texture;

import io.hostilerobot.ceramicrelief.imesh.IMesh;
import io.hostilerobot.ceramicrelief.util.Hash;

import java.util.Objects;

public class TEdge  {
    public TEdge(int v1, int v2) {
        this.v1 = v1;
        this.v2 = v2;
    }
    private final int v1;
    private final int v2;

    public int getV1() { return v1; }
    public int getV2() {
        return v2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TEdge tEdge = (TEdge) o;

        // it's the same edge if we're switching up the order
        return (getV1() == tEdge.getV1() && getV2() == tEdge.getV2())
                || (getV1() == tEdge.getV2() && getV2() == tEdge.getV1());
    }

    @Override
    public int hashCode() {
        return Hash.hashSymmetric(v1, v2);
    }
}

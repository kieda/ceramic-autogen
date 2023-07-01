package io.hostilerobot.ceramicrelief.texture;

import java.util.Objects;

public class TEdge  {
    public TEdge(int v1, int v2) {
        this.v1 = v1;
        this.v2 = v2;
    }
    private int v1;
    private int v2;

    public int getV1() { return v1; }
    public int getV2() {
        return v2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TEdge tEdge = (TEdge) o;
        return v1 == tEdge.v1 && v2 == tEdge.v2;
    }

    @Override
    public int hashCode() {
        return v1 * v2;
    }
}

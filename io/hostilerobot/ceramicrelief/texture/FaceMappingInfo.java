package io.hostilerobot.ceramicrelief.texture;

import java.util.Arrays;

class FaceMappingInfo {
    private final int[] tFaces;
    public FaceMappingInfo(int size) {
        tFaces = new int[size];
        Arrays.fill(tFaces, -1);
    }

    public int getTFace(int meshId) {
        return tFaces[meshId];
    }
    public boolean isFacePlacedOnTexture(int id) {
        return tFaces[id] >= 0;
    }

    void setTFace(int meshId, int tFaceId) {
        tFaces[meshId] = tFaceId;
    }
}

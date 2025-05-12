package io.hostilerobot.ceramicrelief.texture.projection;

import java.util.Arrays;

/**
 * Just a very simple mapping going from the 3d mesh ID -> 2d texture face ID
 * Mapping should be contiguous starting from 0
 */
public class FaceMappingInfo {
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

    public void setTFace(int meshId, int tFaceId) {
        tFaces[meshId] = tFaceId;
    }
}

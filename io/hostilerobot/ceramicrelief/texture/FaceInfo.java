package io.hostilerobot.ceramicrelief.texture;

class FaceInfo {
    private final int faceOrder; // this is used internally as a method to distinguish 3d faces in the heap during our traversal
    private int tFace = -1; // the position in the texture. If negative, position is TBD
    public FaceInfo(int faceOrder) {
        this.faceOrder = faceOrder;
    }
    void setTFace(int face) {
        this.tFace = face;
    }
    public int getFaceOrder() {
        return faceOrder;
    }
    public int getTFace() {
        return tFace;
    }
    public boolean isFacePlacedOnTexture() {
        return tFace >= 0;
    }
}

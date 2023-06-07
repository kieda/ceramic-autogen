package io.hostilerobot.ceramicrelief.texture;

import java.awt.image.BufferedImage;

// create a buffered image such that certain portions may wrap upon itself while drawing a graphics context
//
public class WrappableBufferedImage extends BufferedImage {
    // 1. have a sequence of boundaries in the buffered image (parts that are drawable or not)
    // 2. define edges on the buffered image
    // 3. define which edges are attached or wrapping to other edges.
    // 4. define which edges are dangling, or not attached or wrapping to other edges.
    // have a list of edges that wrap to another that are non-ad
    public WrappableBufferedImage(int width, int height, int imageType) {
        super(width, height, imageType);
    }
}

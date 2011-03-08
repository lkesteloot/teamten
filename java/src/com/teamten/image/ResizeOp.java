// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

import java.awt.image.BufferedImage;

/**
 * Operation to resize and image. Uses a high-quality fast two-pass algorithm.
 */
public class ResizeOp extends AbstractBufferedImageOp {
    private final int mDestWidth;
    private final int mDestHeight;

    public ResizeOp(int destWidth, int destHeight) {
        mDestWidth = destWidth;
        mDestHeight = destHeight;
    }

    @Override // BufferedImageOp
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        // Intermediate image.
        int intWidth = srcWidth;
        int intHeight = srcHeight;

        // Pick first direction to minimize size of intermediate image.
        if ((long) srcWidth*mDestHeight < (long) mDestWidth*srcHeight) {
            intHeight = mDestHeight;
        } else {
            intWidth = mDestWidth;
        }

        // First pass.
        StretchOp stretchOp = new StretchOp(intWidth, intHeight);
        BufferedImage intermediate = stretchOp.filter(src, null);

        // Second pass.
        stretchOp = new StretchOp(mDestWidth, mDestHeight);
        return stretchOp.filter(intermediate, null);
    }
}

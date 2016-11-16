/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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

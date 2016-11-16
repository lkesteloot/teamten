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

import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ColorModel;

/**
 * Fills in easy methods for BufferedImageOp.
 */
public abstract class AbstractBufferedImageOp implements BufferedImageOp {
    @Override // BufferedImageOp
    public Rectangle2D getBounds2D(BufferedImage src) {
        return new Rectangle(0, 0, src.getWidth(), src.getHeight());
    }

    @Override // BufferedImageOp
    public BufferedImage createCompatibleDestImage(BufferedImage src, ColorModel colorModel) {
        if (colorModel == null) {
            colorModel = src.getColorModel();
        }

        return new BufferedImage(colorModel,
                colorModel.createCompatibleWritableRaster(src.getWidth(), src.getHeight()),
                colorModel.isAlphaPremultiplied(), null);
    }

    @Override // BufferedImageOp
    public Point2D getPoint2D(Point2D srcPt, Point2D dstPt) {
        return (Point2D) srcPt.clone();
    }

    @Override // BufferedImageOp
    public RenderingHints getRenderingHints() {
        return null;
    }
}

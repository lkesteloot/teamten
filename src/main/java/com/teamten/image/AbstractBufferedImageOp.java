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

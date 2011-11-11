// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

import java.awt.image.BufferedImage;

/**
 * Turns a photo into a sketchy drawing.
 */
public class Sketchy {
    public BufferedImage run(BufferedImage image) {
        // Convert to black and white.
        image = ImageUtils.toGrayscale(image);

        // Blur slightly.
        image = ImageUtils.blur(image, 2);

        // Threshold.
        image = ImageUtils.quantize(image, new int[] { 50, 120, 190, 255 } );

        return image;
    }
}

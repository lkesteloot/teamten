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
        BufferedImage edges = ImageUtils.blur(image, 2);

        // Find edges.
        edges = ImageUtils.findEdges(edges);

        // Invert.
        image = ImageUtils.invert(edges);

        return image;
    }
}

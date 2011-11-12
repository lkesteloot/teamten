// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Turns a photo into a sketchy drawing.
 */
public class Sketchy {
    public BufferedImage run(BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        int pixelCount = width*height;
        int bytesPerPixel = ImageUtils.getBytesPerPixel(image);

        // Convert to black and white.
        image = ImageUtils.toGrayscale(image);

        // Blur slightly.
        image = ImageUtils.blur(image, 2);

        byte[] data = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();

        for (int i = 0; i < pixelCount; i++) {
            int x = i % width;
            int y = i / width;

            int index = i*bytesPerPixel;
            double gray = ((int) data[index] & 0xFF)/255.0;

            // Increase contrast.
            double contrast = 2.0;
            gray = (gray - 0.5)*contrast + 0.5;
            gray = Math.max(0, Math.min(1, gray));

            // gray = 0..1
            double value = crosshatch(x, y, gray);
            value = Math.max(0, Math.min(1, value));

            byte out = (byte) (int) (value*255.5);

            data[index + 0] = out;
            data[index + 1] = out;
            data[index + 2] = out;
        }

        return image;
    }

    private double crosshatch(int x, int y, double gray) {
        // gray 0..1

        double offset = 0.5;
        double scale = 0.5;

        double copperplate = Math.sin((x + y)/5.0)/2.1 + 0.5;
        // copperplate 0..1

        if (gray <= copperplate) {
            return 0.0;
        } else {
            return 1.0;
        }
    }
}

// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Turns a photo into an engraved drawing.
 */
public class Engraver {
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

            // Normalized coordinates.
            double fx = (double) x / width;
            double fy = (double) y / width; // Keep aspect ratio.

            // Coons patch.
            fx += Math.sin(fy*10)*0.05;

            // Get source level.
            int index = i*bytesPerPixel;
            double gray = ((int) data[index] & 0xFF)/255.0;

            // Increase contrast.
            double contrast = 2.0;
            gray = (gray - 0.5)*contrast + 0.5;
            gray = Math.max(0, Math.min(1, gray));

            // gray = 0..1
            double value = pattern(fx, fy, gray);
            value = Math.max(0, Math.min(1, value));

            byte out = (byte) (int) (value*255.5);

            data[index + 0] = out;
            data[index + 1] = out;
            data[index + 2] = out;
        }

        return image;
    }

    private double pattern(double x, double y, double gray) {
        // x,y 0..1 (roughly)
        // gray 0..1

        double t = (x + y)*2881.0/5.0;

        double copperplate = Math.sin(t)/2.1 + 0.5;
        // copperplate 0..1

        if (gray <= copperplate) {
            return 0.0;
        } else {
            return 1.0;
        }
    }
}

// Copyright 2010 Lawrence Kesteloot.

package com.teamten.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * Like ConvolveOp, this convolves a kernel with an image. But this class assumes that
 * the filter is linearly decomposable, and performs the operation in two passes.
 */
public class DecomposableConvolveOp extends AbstractBufferedImageOp {
    private final double[] mKernel;

    /**
     * The kernel is the horizontal or vertical cross-section of the 2D kernel
     * through its center. The kernel should have an odd length.
     */
    public DecomposableConvolveOp(double[] kernel) {
        assert kernel.length % 2 != 0;

        mKernel = kernel;
    }

    @Override // BufferedImageOp
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        // Just assert, we don't want to silently convert.
        assert src.getType() == BufferedImage.TYPE_3BYTE_BGR
            || src.getType() == BufferedImage.TYPE_4BYTE_ABGR;

        // Apply the kernel twice, once in each direction. Each convolve() call both
        // applies the kernel horizontally and transposes the image.
        return convolve(convolve(src));
    }

    /**
     * Convolves horizontally and transposes the output image.
     */
    private BufferedImage convolve(BufferedImage src) {
        int bytesPerPixel = src.getColorModel().getNumComponents();

        int width = src.getWidth();
        int height = src.getHeight();

        // Make a destination image of the swapped dimensions.
        BufferedImage dest = new BufferedImage(height, width, src.getType());

        // Get the raw underlying bytes. In principle this might break if the
        // data buffer is in multiple banks.
        byte[] srcData = ((DataBufferByte) src.getRaster().getDataBuffer()).getData();
        byte[] destData = ((DataBufferByte) dest.getRaster().getDataBuffer()).getData();

        // Verify that it's the size we expect.
        assert width*height*bytesPerPixel == srcData.length;
        assert height*width*bytesPerPixel == destData.length;

        // The number of bytes per row.
        int srcStride = width*bytesPerPixel;
        int destStride = height*bytesPerPixel;

        // Assume the kernel length is odd.
        int filterRadius = (mKernel.length - 1) / 2;

        // Go through every row of the source image.
        for (int yy = 0; yy < height; yy++) {
            // Go through every pixel of the source row.
            for (int xx = 0; xx < width; xx++) {
                // For every color component.
                for (int b = 0; b < bytesPerPixel; b++) {
                    // Apply the convolution.
                    double sum = 0.0;
                    for (int i = 0; i < mKernel.length; i++) {
                        // Position in source image.
                        int x = xx - filterRadius + i;

                        // Treat off-image pixels as their closest pixel in the image.
                        int clampedX;
                        if (x < 0) {
                            clampedX = 0;
                        } else if (x > width - 1) {
                            clampedX = width - 1;
                        } else {
                            clampedX = x;
                        }

                        int byteIndex = yy*srcStride + clampedX*bytesPerPixel + b;
                        int pixel = (int) srcData[byteIndex] & 0xFF;
                        sum += pixel * mKernel[i];
                    }

                    // Clamp color for writing to byte.
                    int result;
                    if (sum < 0.5) {
                        result = 0;
                    } else if (sum >= 255.0) {
                        result = 255;
                    } else {
                        result = (int) sum;
                    }

                    // Write transposed into destination image.
                    destData[xx*destStride + yy*bytesPerPixel + b] = (byte) result;
                }
            }
        }

        return dest;
    }

    /**
     * Returns a Gaussian kernel of the specified radius, where the radius represents
     * one sigma (standard deviation).
     */
    public static double[] makeGaussianKernel(double radius) {
        // This matches Photoshop.
        double sigma = radius;

        // Go to 3 sigma, where it's pretty much zero.
        int arrayHalfSize = (int) Math.ceil(radius*3);
        int arraySize = arrayHalfSize*2 + 1;
        double[] kernel = new double[arraySize];

        // Precompute constant.
        double twoSigmaSquared = 2*sigma*sigma;

        // Keep track of total so we can normalize.
        double total = 0;
        int index = 0;

        // Walk the kernel.
        for (int i = -arrayHalfSize; i <= arrayHalfSize; i++) {
            // Distance from center.
            double distanceSquared = i*i;

            // Don't bother dividing by constant, we normalize anyway.
            double value = Math.exp(-distanceSquared / twoSigmaSquared);
            kernel[index] = value;

            total += value;
            index++;
        }

        // Normalize so area is 1.0.
        for (int i = 0; i < arraySize; i++) {
            kernel[i] /= total;
        }

        return kernel;
    }
}

// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

/**
 * An image operation that can only stretch in one direction. Uses the Lanczos filter
 * to get fast and high-quality results. This algorithm was taken from the Python Imaging
 * Library's Antialias.c file.
 */
public class StretchOp extends AbstractBufferedImageOp {
    private static final double LANCZOS_FILTER_RADIUS = 3;
    private final int mDestWidth;
    private final int mDestHeight;

    /**
     * Specifies the size of the resulting image. Either the width or the height must
     * match the source.
     */
    public StretchOp(int destWidth, int destHeight) {
        mDestWidth = destWidth;
        mDestHeight = destHeight;
    }

    @Override // BufferedImageOp
    public BufferedImage filter(BufferedImage src, BufferedImage dest) {
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();
        int bytesPerPixel = ImageUtils.getBytesPerPixel(src);

        // Figure out how much we're scaling. 0.5 means shrinking by half, 2.0 means
        // doubling in size. It's the value you divide the destination pixel
        // location by to get the source pixel location.
        double scale;
        if (srcHeight == mDestHeight) {
            // Prepare for horizontal stretch.
            scale = (double) mDestWidth / srcWidth;
        } else if (srcWidth == mDestWidth) {
            // Prepare for vertical stretch.
            scale = (double) mDestHeight / srcHeight;
        } else {
            throw new IllegalArgumentException(
                    "Can only stretch in one direction. Use ResizeOp instead.");
        }

        // We always pass in null, so I don't know what would happen if we didn't.
        // I suppose we'd have to create this exact image.
        if (dest == null) {
            dest = new BufferedImage(mDestWidth, mDestHeight, src.getType());
        }

        // Get the raw underlying bytes. In principle this might break if the
        // data buffer is in multiple banks.
        byte[] srcData = ((DataBufferByte) src.getRaster().getDataBuffer()).getData();
        byte[] destData = ((DataBufferByte) dest.getRaster().getDataBuffer()).getData();

        // Verify that it's the size we expect.
        assert srcWidth*srcHeight*bytesPerPixel == srcData.length;
        assert mDestWidth*mDestHeight*bytesPerPixel == destData.length;

        // The number of bytes per row.
        int srcStride = srcWidth*bytesPerPixel;
        int destStride = mDestWidth*bytesPerPixel;

        // The half size of filter width (distance from center to edge).
        double filterRadius;
        // We want to scale the filter, so we calculate the inverse of this scale and
        // multiple the input to the filter by that.
        double invFilterScale;
        if (scale > 1.0) {
            // If expanding, we don't want the filter to get any smaller than
            // its default filter radius, so we hard-code it to half a pixel with no
            // scaling.
            filterRadius = 0.5;
            invFilterScale = 1.0;
        } else {
            // If shrinking, use a wide filter that covers many pixels in the source image.
            // The more we shrink (smaller "scale"), the bigger the filter (area we're
            // sampling from).
            filterRadius = LANCZOS_FILTER_RADIUS / scale;
            invFilterScale = scale;
        }

        // Coefficient buffer (with rounding safety margin).
        double[] filter = new double[(int) filterRadius*2 + 10];

        if (srcWidth == mDestWidth) {
            verticalStretch(srcData, destData, scale, invFilterScale, filterRadius, filter,
                    srcHeight, bytesPerPixel, srcStride, destStride);
        } else {
            horizontalStretch(srcData, destData, scale, invFilterScale, filterRadius, filter,
                    srcWidth, bytesPerPixel, srcStride, destStride);
        }

        return dest;
    }

    /**
     * Stretches the image vertically given the stretching parameters.
     */
    private void verticalStretch(byte[] srcData, byte[] destData,
            double scale, double invFilterScale, double filterRadius, double[] filter,
            int srcHeight, int bytesPerPixel, int srcStride, int destStride) {

        // Go down the destination image. We use this as the outer loop because
        // for given destination row we must calculate the filter profile (including
        // clipping at the top and bottom edges) and we can reuse it for a whole row.
        for (int destY = 0; destY < mDestHeight; destY++) {
            // The center of the filtered region in the source.
            double srcCenterY = (destY + 0.5)/scale;

            // Calculate the extends of the filtering region in the source,
            // clamping at the edges of the window. beginY is inclusive, endY
            // exclusive.
            int beginY = Math.max(0, (int) Math.floor(srcCenterY - filterRadius));
            int endY = Math.min(srcHeight, (int) Math.ceil(srcCenterY + filterRadius));

            // Calculate the profile of the filter.
            makeFilter(beginY, endY, srcCenterY, invFilterScale, filter);

            // Go through the width of the image (which is the same in the
            // source and destination) and each sub-color component.
            for (int x = 0; x < mDestWidth*bytesPerPixel; x++) {
                // Apply filter to this column and sub-color.
                double result = 0.0;
                for (int srcY = beginY; srcY < endY; srcY++) {
                    result += ((int) srcData[srcY*srcStride + x] & 0xFF)*filter[srcY - beginY];
                }

                // Round to nearest value.
                result += 0.5;

                // Clamp and convert to int.
                int intResult;
                if (result < 0.5) {
                    intResult = 0;
                } else if (result >= 255.0) {
                    intResult = 255;
                } else {
                    intResult = (int) result;
                }

                // Write to destination image.
                destData[destY*destStride + x] = (byte) intResult;
            }
        }
    }

    /**
     * Stretches the image horizontally given the stretching parameters.
     */
    private void horizontalStretch(byte[] srcData, byte[] destData,
            double scale, double invFilterScale, double filterRadius, double[] filter,
            int srcWidth, int bytesPerPixel, int srcStride, int destStride) {

        // Go across the destination image. We use this as the outer loop because
        // for given destination column we must calculate the filter profile (including
        // clipping at the left and right edges) and we can reuse it for a whole column.
        for (int destX = 0; destX < mDestWidth; destX++) {
            double srcCenterX = (destX + 0.5)/scale;

            // Calculate the extends of the filtering region in the source,
            // clamping at the edges of the window. beginX is inclusive, endX
            // exclusive.
            int beginX = Math.max(0, (int) Math.floor(srcCenterX - filterRadius));
            int endX = Math.min(srcWidth, (int) Math.ceil(srcCenterX + filterRadius));

            // Calculate the profile of the filter.
            makeFilter(beginX, endX, srcCenterX, invFilterScale, filter);

            // Go through the height of the image (which is the same in the
            // source and destination).
            for (int y = 0; y < mDestHeight; y++) {
                // Go through each sub-color.
                for (int b = 0; b < bytesPerPixel; b++) {
                    double result = 0.0;
                    for (int srcX = beginX; srcX < endX; srcX++) {
                        result += ((int) srcData[y*srcStride + srcX*bytesPerPixel + b] & 0xFF)
                            *filter[srcX - beginX];
                    }

                    // Round to nearest value.
                    result += 0.5;

                    // Clamp and convert to int.
                    int intResult;
                    if (result < 0.5) {
                        intResult = 0;
                    } else if (result >= 255.0) {
                        intResult = 255;
                    } else {
                        intResult = (int) result;
                    }

                    // Write to destination image.
                    destData[y*destStride + destX*bytesPerPixel + b] = (byte) intResult;
                }
            }
        }
    }

    /**
     * Given an extent and a center within that extent, and a filter scale, calculates 
     * the filter along that extent. The area of the filter is normalized to 1.0.
     *
     * @param begin start of the extend, inclusive.
     * @param end end of the extend, exclusive.
     * @param center the center of the filter, between begin and end.
     * @param invFilterScale reciprocal of how much we scale the filter by.
     * @param filter the first (end - begin) entries of this array are filled
     * with the filter coefficients.
     */
    private static void makeFilter(int begin, int end, double center, double invFilterScale,
            double filter[]) {

        // Calculate filter weights and total area under them.
        double filterArea = 0.0;
        for (int src = begin; src < end; src++) {
            double weight = lanczosFilter((src + 0.5 - center)*invFilterScale);
            filter[src - begin] = weight;
            filterArea += weight;
        }

        // Normalize the filter area to 1.0.
        if (filterArea != 0.0) {
            double invFilterArea = 1.0 / filterArea;
            for (int i = 0; i < end - begin; i++) {
                filter[i] *= invFilterArea;
            }
        }
    }

    /**
     * The normalized sinc function.
     */
    private static double sincFilter(double x) {
        if (x == 0) {
            return 1;
        }

        // This is a normalized sinc.
        x *= Math.PI;

        return Math.sin(x) / x;
    }

    /**
     * The Lanczos filter clipped to three lobes.
     */
    private static double lanczosFilter(double x) {
        if (-3.0 <= x && x < 3.0) {
            return sincFilter(x) * sincFilter(x/3);
        } else {
            return 0.0;
        }
    }
}

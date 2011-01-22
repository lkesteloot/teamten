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
    private static final double LANCZOS_SUPPORT = 3;
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
        int bytesPerPixel;

        if (src.getType() == BufferedImage.TYPE_3BYTE_BGR) {
            bytesPerPixel = 3;
        } else if (src.getType() == BufferedImage.TYPE_4BYTE_ABGR) {
            bytesPerPixel = 4;
        } else {
            throw new IllegalArgumentException("Stretched images must be BGR or ABGR");
        }

        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        // Figure out how much we're scaling. This is positive if we're shrinking.
        double scale;
        double filterScale;
        if (srcHeight == mDestHeight) {
            // Prepare for horizontal stretch.
            filterScale = scale = (double) srcWidth / mDestWidth;
        } else if (srcWidth == mDestWidth) {
            // Prepare for vertical stretch.
            filterScale = scale = (double) srcHeight / mDestHeight;
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

        // Determine support size (length of resampling filter).
        double support = LANCZOS_SUPPORT;
        if (filterScale < 1.0) {
            filterScale = 1.0;
            support = 0.5;
        }
        support *= filterScale;

        // Coefficient buffer (with rounding safety margin).
        double[] k = new double[(int) support * 2 + 10];

        // Vertical stretch.
        if (srcWidth == mDestWidth) {
            for (int yy = 0; yy < mDestHeight; yy++) {
                double center = (yy + 0.5f) * scale;
                double ww = 0.0;
                double ss = 1.0 / filterScale;

                // Calculate filter weights.
                double ymin = Math.floor(center - support);
                if (ymin < 0.0) {
                    ymin = 0.0;
                }
                double ymax = Math.ceil(center + support);
                if (ymax > srcHeight) {
                    ymax = srcHeight;
                }

                for (int y = (int) ymin; y < (int) ymax; y++) {
                    double w = lanczosFilter((y - center + 0.5) * ss) * ss;
                    k[y - (int) ymin] = w;
                    ww = ww + w;
                }
                if (ww == 0.0) {
                    ww = 1.0;
                } else {
                    ww = 1.0 / ww;
                }

                for (int xx = 0; xx < mDestWidth*bytesPerPixel; xx++) {
                    ss = 0.0;
                    for (int y = (int) ymin; y < (int) ymax; y++) {
                        ss = ss + ((int) srcData[y*srcStride + xx] & 0xFF) * k[y-(int) ymin];
                    }
                    ss = ss * ww + 0.5;

                    int result;
                    if (ss < 0.5) {
                        result = 0;
                    } else if (ss >= 255.0) {
                        result = 255;
                    } else {
                        result = (int) ss;
                    }

                    destData[yy*destStride + xx] = (byte) result;
                }
            }
        } else {
            // Horizontal stretch.
            for (int xx = 0; xx < mDestWidth; xx++) {
                double center = (xx + 0.5) * scale;
                double ww = 0.0;
                double ss = 1.0 / filterScale;

                double xmin = Math.floor(center - support);
                if (xmin < 0.0) {
                    xmin = 0.0;
                }
                double xmax = Math.ceil(center + support);
                if (xmax > srcWidth) {
                    xmax = srcWidth;
                }

                for (int x = (int) xmin; x < (int) xmax; x++) {
                    double w = lanczosFilter((x - center + 0.5) * ss) * ss;
                    k[x - (int) xmin] = w;
                    ww = ww + w;
                }
                if (ww == 0.0) {
                    ww = 1.0;
                } else {
                    ww = 1.0 / ww;
                }

                for (int yy = 0; yy < mDestHeight; yy++) {
                    for (int b = 0; b < bytesPerPixel; b++) {
                        ss = 0.0;
                        for (int x = (int) xmin; x < (int) xmax; x++) {
                            ss = ss + ((int) srcData[yy*srcStride + x*bytesPerPixel + b] & 0xFF)
                                * k[x - (int) xmin];
                        }
                        ss = ss * ww + 0.5;

                        int result;
                        if (ss < 0.5) {
                            result = 0;
                        } else if (ss >= 255.0) {
                            result = 255;
                        } else {
                            result = (int) ss;
                        }

                        destData[yy*destStride + xx*bytesPerPixel + b] = (byte) result;
                    }
                }
            }
        }

        return dest;
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

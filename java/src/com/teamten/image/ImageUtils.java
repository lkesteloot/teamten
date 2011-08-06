// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.Kernel;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;

import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

/**
 * Assorted utility methods for transforming images.
 */
public class ImageUtils {
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    /**
     * Create a new image of the given size with all pixels transparent.
     */
    public static BufferedImage makeTransparent(int width, int height) {
        log("Making a transparent image (%dx%d)", width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D g = createGraphics(image);
        g.setBackground(TRANSPARENT);
        g.clearRect(0, 0, width, height);
        g.dispose();

        return image;
    }

    /**
     * Create a new image of the given size with a given background color.
     */
    public static BufferedImage make(int width, int height, Color color) {
        log("Making an image of color %s (%dx%d)", color, width, height);

        int type;
        if (color.getTransparency() == Transparency.TRANSLUCENT) {
            type = BufferedImage.TYPE_4BYTE_ABGR;
        } else {
            type = BufferedImage.TYPE_3BYTE_BGR;
        }

        BufferedImage image = new BufferedImage(width, height, type);

        Graphics2D g = createGraphics(image);
        g.setBackground(color);
        g.clearRect(0, 0, width, height);
        g.dispose();

        return image;
    }

    /**
     * Create a new image of the given size with a white background.
     */
    public static BufferedImage makeWhite(int width, int height) {
        return make(width, height, Color.WHITE);
    }

    /**
     * Creates an image of the specified size with a linear gradient going from begin
     * to end, interpolating the specified colors linearly. Pixels
     * past the end of the line are the color of that end.
     */
    public static BufferedImage makeLinearGradient(int width, int height,
            int beginX, int beginY, Color beginColor, int endX, int endY, Color endColor) {

        log("Making a linear gradient image (%dx%d)", width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        double length = getDistance(beginX, beginY, endX, endY);
        double vx = (endX - beginX) / length;
        double vy = (endY - beginY) / length;

        int beginR = beginColor.getRed();
        int beginG = beginColor.getGreen();
        int beginB = beginColor.getBlue();
        int endR = endColor.getRed();
        int endG = endColor.getGreen();
        int endB = endColor.getBlue();
        int diffR = endR - beginR;
        int diffG = endG - beginG;
        int diffB = endB - beginB;

        for (int y = 0; y < height; y++) {
            int dy = y - beginY;

            for (int x = 0; x < width; x++) {
                int dx = x - beginX;
                double dot = (dx*vx + dy*vy) / length;

                // In per-mil of the radius.
                int dist = (int) (Math.min(Math.max(dot, 0.0), 1.0) * 1000.0);

                int r = beginR + dist*diffR/1000;
                int g = beginG + dist*diffG/1000;
                int b = beginB + dist*diffB/1000;

                int rgb = (r << 16) | (g << 8) | b;

                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    /**
     * Creates an image of the specified size with a circular gradient going from center
     * to the edge (of the circle), interpolating the specified colors linearly. Pixels
     * past the edge of the circle are the color of the edge.
     */
    public static BufferedImage makeCircularGradient(int width, int height,
            int centerX, int centerY, Color centerColor, int edgeX, int edgeY, Color edgeColor) {

        log("Making a circular gradient image (%dx%d)", width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_3BYTE_BGR);

        double radius = getDistance(centerX, centerY, edgeX, edgeY);

        int centerR = centerColor.getRed();
        int centerG = centerColor.getGreen();
        int centerB = centerColor.getBlue();
        int edgeR = edgeColor.getRed();
        int edgeG = edgeColor.getGreen();
        int edgeB = edgeColor.getBlue();
        int diffR = edgeR - centerR;
        int diffG = edgeG - centerG;
        int diffB = edgeB - centerB;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                // In per-mil of the radius.
                int dist = (int) (Math.min(getDistance(x, y, centerX, centerY)/radius, 1.0)
                        * 1000.0);

                int r = centerR + dist*diffR/1000;
                int g = centerG + dist*diffG/1000;
                int b = centerB + dist*diffB/1000;

                int rgb = (r << 16) | (g << 8) | b;

                image.setRGB(x, y, rgb);
            }
        }

        return image;
    }

    /**
     * Returns the distance between two pixels.
     */
    private static double getDistance(int x1, int y1, int x2, int y2) {
        int dx = x1 - x2;
        int dy = y1 - y2;

        return Math.sqrt(dx*dx + dy*dy);
    }

    /**
     * Creates a high-quality graphics objects for this object.
     */
    public static Graphics2D createGraphics(BufferedImage image) {
        Graphics2D g = image.createGraphics();

        g.setRenderingHints(getHighQualityRenderingMap());

        return g;
    }

    /**
     * Return the image with a new type.
     */
    public static BufferedImage convertType(BufferedImage src, int newType) {
        log("Converting image to type %d (%dx%d)", newType, src.getWidth(), src.getHeight());

        BufferedImage dest = new BufferedImage(src.getWidth(), src.getHeight(), newType);
        pasteInto(dest, src, 0, 0);
        return dest;
    }

    /**
     * Make a copy of the image.
     */
    public static BufferedImage copy(BufferedImage image) {
        return convertType(image, image.getType());
    }

    /**
     * Compose images over one another. The first layer in the array is the lowest one.
     */
    public static BufferedImage compose(BufferedImage ... layers) {
        int width = 0;
        int height = 0;

        for (BufferedImage layer : layers) {
            width = Math.max(width, layer.getWidth());
            height = Math.max(height, layer.getHeight());
        }

        log("Composing %d images (%dx%d)", layers.length, width, height);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);

        Graphics2D g = createGraphics(image);
        for (BufferedImage layer : layers) {
            g.drawImage(layer, 0, 0, null);
        }
        g.dispose();

        return image;
    }

    /**
     * Returns the shadow of the input image. The shadow is based only on the
     * alpha channel.
     *
     * @param radius the size of the shadow, in pixels.
     * @param darkness how dark to make the shadow, where 0.0 means
     * none and 1.0 is the darkest.
     */
    public static BufferedImage makeShadow(BufferedImage image, double radius, double darkness) {
        log("Making a shadow of radius %g and darkness %g", radius, darkness);

        BufferedImage shadow = new BufferedImage(image.getWidth(), image.getHeight(),
                BufferedImage.TYPE_4BYTE_ABGR);

        // Make an opaque image where gray = alpha of original.
        int width = shadow.getWidth();
        int height = shadow.getHeight();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = image.getRGB(x, y);
                int alpha = argb >>> 24;

                argb = 0xFF000000 | (alpha << 16) | (alpha << 8) | (alpha << 0);

                shadow.setRGB(x, y, argb);
            }
        }

        // Blur that.
        shadow = blur(shadow, radius);

        // Make a semi-transparent image where the color is black and the alpha is based
        // on the blurred color above.
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = shadow.getRGB(x, y);

                int alpha = argb & 0xFF;

                // Darken or lighten shadow.
                alpha = (int) (alpha * darkness);
                alpha = Math.min(Math.max(alpha, 0), 255);

                // Not premultiplied, so we can just set color to black.
                argb = alpha << 24;

                shadow.setRGB(x, y, argb);
            }
        }

        return shadow;
    }

    /**
     * Return an image with the original and its reflection.
     *
     * @param reflectionHeightFraction the fraction of the original height to make visible
     * in the reflection. 0.2 is a good value here.
     */
    public static BufferedImage addReflection(BufferedImage image,
            double reflectionHeightFraction) {

        log("Adding a reflection");

        // Input image size.
        int width = image.getWidth();
        int height = image.getHeight();

        // Add reflection.
        int reflectionHeight = (int) (height * reflectionHeightFraction);
        int fullHeight = height + reflectionHeight;

        // Compose the full image.
        BufferedImage fullImage = new BufferedImage(width, fullHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = createGraphics(fullImage);

        // Draw original.
        g.drawImage(image, 0, 0, null);

        // Draw reflection.
        g.drawImage(flipVertically(image), 0, height, null);

        // Fade out the reflection.
        int luminanceTop = 128;
        int luminanceBottom = 255;
        double luminanceExp = 0.5;

        for (int y = 0; y < reflectionHeight; y++) {
            double t = (float) y / (reflectionHeight - 1);

            t = Math.pow(t, luminanceExp);

            int luminance = luminanceTop + (int) (t*(luminanceBottom - luminanceTop));

            g.setColor(new Color(255, 255, 255, luminance));
            g.drawLine(0, height + y, width - 1, height + y);
        }
        g.dispose();

        return fullImage;
    }

    /**
     * Blurs an image using a high-quality high-speed two-pass algorithm.
     * Good stuff about blurring: http://www.jhlabs.com/ip/blurring.html
     */
    public static BufferedImage blur(BufferedImage image, double radius) {
        log("Blurring with radius %g", radius);

        DecomposableConvolveOp op = new DecomposableConvolveOp(
                DecomposableConvolveOp.makeGaussianKernel(radius));
        return op.filter(image, null);
    }

    /**
     * Returns a copy of the image, flipped vertically.
     */
    public static BufferedImage flipVertically(BufferedImage image) {
        log("Flipping vertically");

        AffineTransformOp op = new AffineTransformOp(
                new AffineTransform(1.0, 0.0, 0.0, -1.0, 0.0, image.getHeight()),
                getHighQualityRenderingHints());
        return op.filter(image, null);
    }

    /**
     * Returns a copy of the image, flipped horizontally.
     */
    public static BufferedImage flipHorizontally(BufferedImage image) {
        log("Flipping horizontally");

        AffineTransformOp op = new AffineTransformOp(
                new AffineTransform(-1.0, 0.0, 0.0, 1.0, image.getWidth(), 0.0),
                getHighQualityRenderingHints());
        return op.filter(image, null);
    }

    /**
     * Adds a margin to the image. The margin will be of the specified color,
     * unless the color, unless the color is null, in which case the margin
     * will be transparent.
     */
    public static BufferedImage addMargin(BufferedImage src,
            int top, int right, int bottom, int left, Color color) {

        log("Adding a margin (%d,%d,%d,%d)", top, right, bottom, left);

        int width = src.getWidth() + left + right;
        int height = src.getHeight() + top + bottom;
        BufferedImage dest;

        if (color == null) {
            dest = makeTransparent(width, height);
        } else {
            dest = make(width, height, color);
        }

        pasteInto(dest, src, left, top);

        return dest;
    }

    /**
     * Returns a resized image.
     */
    public static BufferedImage resize(BufferedImage image, int width, int height) {
        log("Resizing from (%dx%d) to (%dx%d)",
                image.getWidth(), image.getHeight(), width, height);

        ResizeOp op = new ResizeOp(width, height);
        return op.filter(image, null);
    }

    /**
     * Returns an image scaled by a particular ratio, where 0.5 means half width and
     * half height.
     */
    public static BufferedImage scale(BufferedImage image, double ratio) {
        return resize(image,
                (int) (image.getWidth()*ratio + 0.5),
                (int) (image.getHeight()*ratio + 0.5));
    }

    /**
     * Returns the image resized to fit in this size but keep the original aspect
     * ratio. Use 0 as either size (but not both) to mean "infinity".
     */
    public static BufferedImage resizeToFit(BufferedImage image, int width, int height) {
        int fitWidth = width;
        int fitHeight = height;

        if (width == 0 && height == 0) {
            throw new IllegalArgumentException("Must specify either width or height");
        }

        if (width != 0 && height != 0) {
            if (image.getWidth() * height < image.getHeight() * width) {
                width = 0;
            } else {
                height = 0;
            }
        }

        if (width == 0) {
            width = image.getWidth() * height / image.getHeight();
        } else {
            height = image.getHeight() * width / image.getWidth();
        }

        log("Resizing from (%dx%d) to fit (%dx%d), final size is (%dx%d)",
                image.getWidth(), image.getHeight(), fitWidth, fitHeight, width, height);

        return resize(image, width, height);
    }

    /**
     * Return the rectangle representing the part of the image that's not like the
     * frame (specifically, not like the upper-left corner).
     *
     * @throws IllegalArgumentException if the entire image is the same color.
     */
    public static Rectangle getTrimmingRectangle(BufferedImage image) {
        // Inclusive rectangle.
        int x1 = 0;
        int y1 = 0;
        int x2 = image.getWidth() - 1;
        int y2 = image.getHeight() - 1;

        // Color we're removing from upper-left corner.
        int trimColor = image.getRGB(0, 0);

        // Move top down.
        while (y1 <= y2 && rowHasColor(image, y1, trimColor)) {
            y1++;
        }

        // Move bottom up.
        while (y2 >= y1 && rowHasColor(image, y2, trimColor)) {
            y2--;
        }

        // Move left right.
        while (x1 <= x2 && columnHasColor(image, x1, trimColor)) {
            x1++;
        }

        // Move right left.
        while (x2 >= x1 && columnHasColor(image, x2, trimColor)) {
            x2--;
        }

        if (y1 > y2 || x1 > x2) {
            throw new IllegalArgumentException("Entire image is the same color");
        }

        return Rectangle.makeFromInclusive(x1, y1, x2, y2);
    }

    /**
     * Removes any constant color around an image. The color is determined by the upper-left
     * pixel. Returns the sub-image, which shares the raster data with the
     * input image.
     *
     * @throws IllegalArgumentException if the entire image is a single color.
     */
    public static BufferedImage trim(BufferedImage image) {
        // Figure out what we're trimming.
        Rectangle rectangle = getTrimmingRectangle(image);

        // Bleah.
        int trimColor = image.getRGB(0, 0);

        log("Trimming (%dx%d) down to %s based on color %s",
                image.getWidth(), image.getHeight(),
                rectangle, new Color(trimColor, true));

        // We copy the subimage because a reference to it causes problems later
        // when scaling. We could later try to get the stride right in StretchOp,
        // though I couldn't immediately figure out how to do that.
        return copy(image.getSubimage(rectangle.getX(), rectangle.getY(),
                    rectangle.getWidth(), rectangle.getHeight()));
    }

    /**
     * Returns true if an entire row has the specified color.
     */
    private static boolean rowHasColor(BufferedImage image, int y, int color) {
        int width = image.getWidth();

        for (int x = 0; x < width; x++) {
            if (image.getRGB(x, y) != color) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns true if an entire column has the specified color.
     */
    private static boolean columnHasColor(BufferedImage image, int x, int color) {
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            if (image.getRGB(x, y) != color) {
                return false;
            }
        }

        return true;
    }

    /**
     * Returns the left part of an image.
     */
    public static BufferedImage left(BufferedImage src, int pixels) {
        BufferedImage dest = new BufferedImage(pixels, src.getHeight(), src.getType());
        pasteInto(dest, src, 0, 0);
        return dest;
    }

    /**
     * Returns the right part of an image.
     */
    public static BufferedImage right(BufferedImage src, int pixels) {
        BufferedImage dest = new BufferedImage(pixels, src.getHeight(), src.getType());
        pasteInto(dest, src, pixels - src.getWidth(), 0);
        return dest;
    }

    /**
     * Returns the top part of an image.
     */
    public static BufferedImage top(BufferedImage src, int pixels) {
        BufferedImage dest = new BufferedImage(src.getWidth(), pixels, src.getType());
        pasteInto(dest, src, 0, 0);
        return dest;
    }

    /**
     * Returns the bottom part of an image.
     */
    public static BufferedImage bottom(BufferedImage src, int pixels) {
        BufferedImage dest = new BufferedImage(src.getWidth(), pixels, src.getType());
        pasteInto(dest, src, 0, pixels - src.getHeight());
        return dest;
    }

    /**
     * Concatenates the images left to right. Images are top-aligned, and the background
     * is initialized to black.
     */
    public static BufferedImage leftToRight(BufferedImage ... layers) {
        int width = 0;
        int height = 0;

        for (BufferedImage layer : layers) {
            width += layer.getWidth();
            height = Math.max(height, layer.getHeight());
        }

        BufferedImage dest = new BufferedImage(width, height, layers[0].getType());
        Graphics2D g = createGraphics(dest);
        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, width, height);
        g.dispose();

        width = 0;
        for (BufferedImage layer : layers) {
            pasteInto(dest, layer, width, 0);
            width += layer.getWidth();
        }

        return dest;
    }

    /**
     * Concatenates the images top to bottom. Images are left-aligned, and the background
     * is initialized to black.
     */
    public static BufferedImage topToBottom(BufferedImage ... layers) {
        int width = 0;
        int height = 0;

        for (BufferedImage layer : layers) {
            width = Math.max(width, layer.getWidth());
            height += layer.getHeight();
        }

        BufferedImage dest = new BufferedImage(width, height, layers[0].getType());
        Graphics2D g = createGraphics(dest);
        g.setBackground(Color.BLACK);
        g.clearRect(0, 0, width, height);
        g.dispose();

        height = 0;
        for (BufferedImage layer : layers) {
            pasteInto(dest, layer, 0, height);
            height += layer.getHeight();
        }

        return dest;
    }

    /**
     * Returns an image with the top image pasted onto the bottom image at the specified
     * location.
     */
    public static BufferedImage pasteAt(BufferedImage bottom, BufferedImage top, int x, int y) {
        BufferedImage dest = copy(bottom);

        pasteInto(dest, top, x, y);

        return dest;
    }

    /**
     * Returns an image with the top image pasted onto the bottom image at the specified
     * location and with the specified blending mode.
     */
    public static BufferedImage pasteAtWith(BufferedImage bottom, BufferedImage top, int x, int y,
            BlendingMode blendingMode) {

        BufferedImage dest;

        switch (blendingMode) {
            case NORMAL:
                dest = copy(bottom);
                pasteInto(dest, top, x, y);
                break;

            case SCREEN:
                dest = copy(bottom);
                Graphics2D g = createGraphics(dest);
                for (int dy = 0; dy < top.getHeight(); dy++) {
                    for (int dx = 0; dx < top.getWidth(); dx++) {
                        int bottomRgb = bottom.getRGB(x + dx, y + dy);
                        int topRgb = top.getRGB(dx, dy);

                        int bottomRed = bottomRgb & 0xFF;
                        int bottomGreen = (bottomRgb >> 8) & 0xFF;
                        int bottomBlue = (bottomRgb >> 16) & 0xFF;
                        // Ignore bottom alpha?
                        /// int bottomAlpha = (bottomRgb >> 24) & 0xFF;

                        int topRed = topRgb & 0xFF;
                        int topGreen = (topRgb >> 8) & 0xFF;
                        int topBlue = (topRgb >> 16) & 0xFF;
                        // Ignoring top alpha until we need it.
                        /// int topAlpha = (topRgb >> 24) & 0xFF;

                        int resultRed = 255 - (255 - bottomRed)*(255 - topRed)/255;
                        int resultGreen = 255 - (255 - bottomGreen)*(255 - topGreen)/255;
                        int resultBlue = 255 - (255 - bottomBlue)*(255 - topBlue)/255;
                        int resultAlpha = 255;

                        int resultRgb = (resultAlpha << 24) | (resultBlue << 16)
                            | (resultGreen << 8) | resultRed;

                        dest.setRGB(x + dx, y + dy, resultRgb);
                    }
                }
                g.dispose();
                break;

            default:
                throw new IllegalArgumentException("Must specify a blending mode.");
        }

        return dest;
    }

    /**
     * Returns an image with the color of the main image (which must be BGR) and the
     * alpha of the mask (which must be ABGR, color is ignored). The two images must
     * be the same size.
     */
    public static BufferedImage clipToMask(BufferedImage image, BufferedImage mask) {
        assert image.getType() == BufferedImage.TYPE_3BYTE_BGR;
        assert mask.getType() == BufferedImage.TYPE_4BYTE_ABGR;
        assert image.getWidth() == mask.getWidth();
        assert image.getHeight() == mask.getHeight();

        BufferedImage output = copy(mask);

        int width = image.getWidth();
        int height = image.getHeight();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int imageRgb = image.getRGB(x, y);
                int outputRgb = output.getRGB(x, y);

                outputRgb = (outputRgb & 0xFF000000) | (imageRgb & 0x00FFFFFF);

                output.setRGB(x, y, outputRgb);
            }
        }

        return output;
    }

    /**
     * Returns an semi-transparent image where the transparency was deduced from the color.
     */
    public static BufferedImage grayToTransparent(BufferedImage image,
            int grayTransparent, int grayOpaque) {

        int width = image.getWidth();
        int height = image.getHeight();

        log("Converting grayscale to transparent (%dx%d), %d to %d", width, height,
                grayTransparent, grayOpaque);

        assert image.getType() == BufferedImage.TYPE_3BYTE_BGR;

        BufferedImage output = makeTransparent(width, height);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = image.getRGB(x, y);

                int red = rgb & 0xFF;
                int green = (rgb >> 8) & 0xFF;
                int blue = (rgb >> 16) & 0xFF;

                int gray = (red*30 + green*59 + blue*11)/100;

                int opacity = (gray - grayTransparent) * 255 / (grayOpaque - grayTransparent);

                rgb = (opacity << 24) | (rgb & 0x00FFFFFF);

                output.setRGB(x, y, rgb);
            }
        }

        return output;

    }

    /**
     * Draws src into dest at x, y.
     */
    private static void pasteInto(BufferedImage dest, BufferedImage src, int x, int y) {
        Graphics2D g = createGraphics(dest);
        g.drawImage(src, x, y, null);
        g.dispose();
    }

    /**
     * Returns an interpolated color, where 0.0 means c1 and 1.0 means c2. Values outside
     * the range 0.0 to 1.1 are valid and will be clamped at 0 and 255 (possibly causing
     * distortion).
     */
    public static Color interpolateColor(Color c1, Color c2, double fraction) {
        int c1R = c1.getRed();
        int c1G = c1.getGreen();
        int c1B = c1.getBlue();
        int c2R = c2.getRed();
        int c2G = c2.getGreen();
        int c2B = c2.getBlue();
        int diffR = c2R - c1R;
        int diffG = c2G - c1G;
        int diffB = c2B - c1B;

        int r = (int) (c1R + fraction*diffR);
        int g = (int) (c1G + fraction*diffG);
        int b = (int) (c1B + fraction*diffB);

        r = Math.min(Math.max(r, 0), 255);
        g = Math.min(Math.max(g, 0), 255);
        b = Math.min(Math.max(b, 0), 255);

        return new Color(r, g, b);
    }

    /**
     * Generate a checkerboard image. The squares are anchored at the upper-left
     * corner of the image.
     *
     * @param width the width of the generated image
     * @param height the height of the generated image
     * @param color1 the color of the square in the upper-left corner
     * @param color2 the alternating color
     * @param squareSize the width and height of the checker square
     */
    public static BufferedImage makeCheckerboard(int width, int height,
            Color color1, Color color2, int squareSize) {

        BufferedImage image = make(width, height, color1);

        Graphics2D g = createGraphics(image);
        g.setPaint(color2);

        for (int y = 0; y < height; y += squareSize) {
            for (int x = y % (squareSize*2); x < width; x += squareSize*2) {
                g.fillRect(x, y, squareSize, squareSize);
            }
        }

        g.dispose();

        return image;
    }

    /**
     * Compose the given image over a checkerboard to look for the transparency area.
     */
    public static BufferedImage composeOverCheckerboard(BufferedImage image) {
        BufferedImage checkerboard = makeCheckerboard(
                image.getWidth(), image.getHeight(),
                new Color(0.65f, 0.65f, 0.65f, 1.0f),
                new Color(0.70f, 0.70f, 0.70f, 1.0f),
                16);

        return compose(checkerboard, image);
    }

    /**
     * Load an image from a filename.
     */
    public static BufferedImage load(String filename) throws IOException {
        BufferedImage image = ImageIO.read(new File(filename));

        // I don't know why this happens, and whether it's okay to always go to BGR.
        if (image.getType() == 0) {
            image = ImageUtils.convertType(image, BufferedImage.TYPE_3BYTE_BGR);
        }

        log("Loaded \"%s\" (%dx%d)", filename, image.getWidth(), image.getHeight());

        return image;
    }

    /**
     * Load an image from an input stream.
     */
    public static BufferedImage load(InputStream inputStream) throws IOException {
        BufferedImage image = ImageIO.read(inputStream);

        // I don't know why this happens, and whether it's okay to always go to BGR.
        if (image.getType() == 0) {
            image = ImageUtils.convertType(image, BufferedImage.TYPE_3BYTE_BGR);
        }

        log("Loaded input stream (%dx%d)", image.getWidth(), image.getHeight());

        return image;
    }

    /**
     * Saves an image to a filename, auto-detecting the type.
     */
    public static void save(BufferedImage image, String filename) throws IOException {
        log("Saving \"%s\" (%dx%d)", filename, image.getWidth(), image.getHeight());

        String fileType;

        if (filename.toLowerCase().endsWith(".png")) {
            fileType = "png";
        } else if (filename.toLowerCase().endsWith(".jpg")
                || filename.toLowerCase().endsWith(".jpeg")) {

            fileType = "jpg";
        } else {
            throw new IllegalArgumentException("File type not supported: " + filename);
        }

        ImageIO.write(image, fileType, new File(filename));
    }

    /**
     * Saves an image to an output stream, auto-detecting the type from the MIME type.
     */
    public static void save(BufferedImage image, OutputStream outputStream, String mimeType)
        throws IOException {

        log("Saving output stream of type \"%s\" (%dx%d)", mimeType,
                image.getWidth(), image.getHeight());

        String fileType;

        if (mimeType.toLowerCase().equals("image/png")) {
            fileType = "png";
        } else if (mimeType.toLowerCase().equals("image/jpg")
                || mimeType.toLowerCase().equals("image/jpeg")) {

            fileType = "jpg";
        } else {
            throw new IllegalArgumentException("Mime type not supported: " + mimeType);
        }

        ImageIO.write(image, fileType, outputStream);
    }

    /**
     * Returns an object setting as many things to high-quality as possible.
     */
    public static RenderingHints getHighQualityRenderingHints() {
        return new RenderingHints(getHighQualityRenderingMap());
    }

    /**
     * Returns a map setting as many things to high-quality as possible.
     */
    public static Map<RenderingHints.Key,Object> getHighQualityRenderingMap() {
        Map<RenderingHints.Key,Object> map = new HashMap<RenderingHints.Key,Object>();

        map.put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        map.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        map.put(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        map.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
        map.put(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
        // This is only supposed to control the spacing and position of characters, but actually
        // makes them look good. Maybe it forces each glyph to be rendered again and it's
        // rendered better?
        map.put(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        return map;
    }

    /**
     * Return a font of the specified typeface and attributes and size.
     */
    public static Font getFont(Typeface typeface, boolean bold, boolean italic, boolean narrow,
            double size) throws FontFormatException, IOException {

        // Figure out which filename to load.
        String filename;

        switch (typeface) {
            case HELVETICA:
                int helveticaNumber = 45
                    + (italic ? 1 : 0)
                    + (narrow ? 2 : 0)
                    + (bold ? 20 : 0);

                filename = "fonts/helr" + helveticaNumber + "w.ttf";
                break;

            case FUTURA:
                /**
                    Florencesans:
                    1 = roman
                    2 = ? (slightly smaller)
                    3 = italic
                    4 = italic lighter
                    5 = very compressed
                    6, 7, 8 = ditto
                    9 = slightly compressed
                    10, 11, 12 = ditto
                    13 = extended
                    14, 15, 16 = ditto
                    +16 = small caps
                    33 = bold
                    34 = bold italic
                    35-36 = small caps
                    37 = outline
                    38 = outline italic
                    39-40 = small caps
                    41 = shadow
                    42 = shadow italic
                    43-44 = small caps
                    45 = reverse italic
                    46 = reverse italic slightly smaller
                */

                int futuraNumber;
                if (bold) {
                    futuraNumber = 33 + (italic ? 1 : 0);
                } else {
                    futuraNumber = 1
                        + (italic ? 2 : 0)
                        + (narrow ? 8 : 0);
                }

                filename = String.format("fonts/Florsn%02d.ttf", futuraNumber);
                break;

            case COURIER:
                if (bold && italic) {
                    filename = "fonts/courbi.ttf";
                } else if (bold) {
                    filename = "fonts/courbd.ttf";
                } else if (italic) {
                    filename = "fonts/couri.ttf";
                } else {
                    filename = "fonts/cour.ttf";
                }
                break;

            default:
                throw new IllegalArgumentException("Unknown typeface " + typeface);
        }

        log("Loading font \"%s\"", filename);

        InputStream inputStream = ImageUtils.class.getResourceAsStream(filename);

        try {
            return Font.createFont(Font.TRUETYPE_FONT, inputStream).deriveFont((float) size);
        } finally {
            inputStream.close();
        }
    }

    /**
     * Prints a formatted line to the console.
     */
    private static void log(String format, Object ... args) {
        System.out.println(String.format(format, args));
    }
}


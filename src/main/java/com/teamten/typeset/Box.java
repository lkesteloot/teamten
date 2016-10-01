
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Represents a 2D box of something, like a word or an image. Can be used by itself to make an empty space.
 */
public class Box extends NonDiscardableElement {
    private final long mWidth;
    private final long mHeight;
    private final long mDepth;

    public Box(long width, long height, long depth) {
        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }

    protected Box(BoxDimensions boxDimensions) {
        mWidth = boxDimensions.mWidth;
        mHeight = boxDimensions.mHeight;
        mDepth = boxDimensions.mDepth;
    }

    /**
     * The width (horizontally) of this box.
     */
    @Override
    public long getWidth() {
        return mWidth;
    }

    /**
     * The distance from the baseline to the top of the box.
     */
    @Override
    public long getHeight() {
        return mHeight;
    }

    /**
     * The distance from the baseline to the bottom of the box.
     */
    @Override
    public long getDepth() {
        return mDepth;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        return mWidth;
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        return mHeight + mDepth;
    }

    /**
     * So we can have a one-arg constructor.
     */
    protected static class BoxDimensions {
        private final long mWidth;
        private final long mHeight;
        private final long mDepth;

        public BoxDimensions(long width, long height, long depth) {
            mWidth = width;
            mHeight = height;
            mDepth = depth;
        }
    }
}

package com.teamten.typeset;

/**
 * Simple class that implements the Dimensions interface.
 */
public class AbstractDimensions implements Dimensions {
    private final long mWidth;
    private final long mHeight;
    private final long mDepth;

    public AbstractDimensions(long width, long height, long depth) {
        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }

    @Override
    public long getWidth() {
        return mWidth;
    }

    @Override
    public long getHeight() {
        return mHeight;
    }

    @Override
    public long getDepth() {
        return mDepth;
    }
}

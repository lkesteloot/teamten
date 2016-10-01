
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Whitespace that has a default width but can be shrunk or stretched.
 */
public class Glue extends DiscardableElement {
    private final long mSize;
    private final long mStretch;
    private final long mShrink;
    private final boolean mIsHorizontal;

    /**
     * All units are in scaled points.
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added.
     * @param shrink the maximum extra space that can be removed.
     */
    public Glue(long size, long stretch, long shrink, boolean isHorizontal) {
        mSize = size;
        mStretch = stretch;
        mShrink = shrink;
        mIsHorizontal = isHorizontal;
    }

    public long getSize() {
        return mSize;
    }

    public long getStretch() {
        return mStretch;
    }

    public long getShrink() {
        return mShrink;
    }

    @Override
    public long getWidth() {
        return mIsHorizontal ? mSize : 0;
    }

    @Override
    public long getHeight() {
        return mIsHorizontal ? 0 : mSize;
    }

    @Override
    public long getDepth() {
        return 0;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        // Assume that we've been "set" and that the stretch or shrink has been determined and put into size.
        return mSize;
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Assume that we've been "set" and that the stretch or shrink has been determined and put into size.
        return mSize;
    }
}

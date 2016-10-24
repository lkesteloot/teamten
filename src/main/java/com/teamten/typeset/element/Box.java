
package com.teamten.typeset.element;

import com.teamten.typeset.Dimensions;
import com.teamten.typeset.PdfUtil;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

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

    protected Box(Dimensions dimensions) {
        this(dimensions.getWidth(), dimensions.getHeight(), dimensions.getDepth());
    }

    /**
     * Draw a debug rectangle at specified point, which represents the left-most point at the baseline.
     */
    protected void drawDebugRectangle(PDPageContentStream contents, long x, long y) throws IOException {
        if (DRAW_DEBUG) {
            PdfUtil.drawDebugRectangle(contents, x, y, getWidth(), getHeight());
            PdfUtil.drawDebugRectangle(contents, x, y - getDepth(), getWidth(), getDepth());
        }
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
     * A string that specifies the three dimensions of the box.
     */

    protected String getDimensionString() {
        return String.format("(%.1fpt, %.1fpt, %.1fpt)", PT.fromSp(mWidth), PT.fromSp(mHeight), PT.fromSp(mDepth));
    }

}

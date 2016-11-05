
package com.teamten.typeset.element;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Whitespace that has a default width but can be shrunk or stretched.
 */
public class Glue extends DiscardableElement {
    private final long mSize;
    private final Flexibility mStretch;
    private final Flexibility mShrink;
    private final boolean mIsHorizontal;

    /**
     * All units are in scaled points.
     *
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added.
     * @param shrink the maximum extra space that can be removed.
     */
    public Glue(long size, Flexibility stretch, Flexibility shrink, boolean isHorizontal) {
        mSize = size;
        mStretch = stretch;
        mShrink = shrink;
        mIsHorizontal = isHorizontal;
    }

    /**
     * All units are in scaled points.
     *
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added (not infinite).
     * @param shrink the maximum extra space that can be removed (not infinite).
     */
    public Glue(long size, long stretch, boolean stretchIsInfinite, long shrink, boolean shrinkIsInfinite, boolean isHorizontal) {
        this(size, new Flexibility(stretch, stretchIsInfinite), new Flexibility(shrink, shrinkIsInfinite), isHorizontal);
    }

    /**
     * Convenience constructor for non-infinite glue. All units are in scaled points.
     *
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added (not infinite).
     * @param shrink the maximum extra space that can be removed (not infinite).
     */
    public Glue(long size, long stretch, long shrink, boolean isHorizontal) {
        this(size, stretch, false, shrink, false, isHorizontal);
    }

    public long getSize() {
        return mSize;
    }

    public Flexibility getStretch() {
        return mStretch;
    }

    public Flexibility getShrink() {
        return mShrink;
    }

    public boolean isHorizontal() {
        return mIsHorizontal;
    }

    /**
     * Fix the glue to the specified size. Subclasses can override to return a copy of themselves.
     */
    public Glue fixed(long newSize) {
        return new Glue(newSize, 0, 0, mIsHorizontal);
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

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%s%s glue: %.1fpt%s%s%n", indent, mIsHorizontal ? "Horizontal" : "Vertical",
                PT.fromSp(mSize), mStretch.toString("+"), mShrink.toString("-"));
    }

    @Override
    public String toTextString() {
        return " ";
    }

}

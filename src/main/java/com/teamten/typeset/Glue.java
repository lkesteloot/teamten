
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Whitespace that has a default width but can be shrunk or stretched.
 */
public class Glue extends DiscardableElement {
    private final long mSize;
    private final Expandability mStretch;
    private final Expandability mShrink;
    private final boolean mIsHorizontal;

    /**
     * All units are in scaled points.
     *
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added.
     * @param shrink the maximum extra space that can be removed.
     */
    private Glue(long size, Expandability stretch, Expandability shrink, boolean isHorizontal) {
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
        this(size, new Expandability(stretch, stretchIsInfinite), new Expandability(shrink, shrinkIsInfinite), isHorizontal);
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

    public Expandability getStretch() {
        return mStretch;
    }

    public Expandability getShrink() {
        return mShrink;
    }

    public boolean isHorizontal() {
        return mIsHorizontal;
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

    public static class Expandability {
        private final long mAmount;
        private final boolean mIsInfinite;

        public Expandability(long amount, boolean isInfinite) {
            mAmount = amount;
            mIsInfinite = isInfinite;
        }

        /**
         * Amount by which we can stretch or shrink.
         */
        public long getAmount() {
            return mAmount;
        }

        /**
         * Whether this is an infinite stretch or shrink, meaning that non-infinite ones don't factor.
         */
        public boolean isInfinite() {
            return mIsInfinite;
        }

        public String toString(String prefix) {
            if (mAmount == 0) {
                return "";
            } else {
                return String.format("%s%.1f%s", prefix, PT.fromSp(mAmount), mIsInfinite ? "inf" : "pt");
            }
        }
    }

    /**
     * Keeps track of a sum of expandability, along with whether the infinite amount trumps the finite one.
     */
    public static class ExpandabilitySum {
        private long mFiniteAmount = 0;
        private long mInfiniteAmount = 0;

        public void add(Expandability expandability) {
            if (expandability.isInfinite()) {
                mInfiniteAmount += expandability.getAmount();
            } else {
                mFiniteAmount += expandability.getAmount();
            }
        }

        /**
         * Whether there was non-zero infinite expandability.
         */
        public boolean isInfinite() {
            return mInfiniteAmount != 0;
        }

        /**
         * The expandability, where any finite amount trumps the finite one.
         */
        public long getAmount() {
            return isInfinite() ? mInfiniteAmount : mFiniteAmount;
        }
    }
}

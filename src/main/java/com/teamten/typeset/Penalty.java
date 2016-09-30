
package com.teamten.typeset;

/**
 * A location where the line can be broken, but with a penalty.
 */
public class Penalty extends Element {
    public static final int INFINITY = 1000;
    public static final int HYPHEN = 100;
    private final float mWidth;
    private final int mPenalty;

    public Penalty(float width, int penalty) {
        mWidth = width;
        mPenalty = penalty;
    }

    public float getWidth() {
        return mWidth;
    }

    public int getPenalty() {
        return mPenalty;
    }

    @Override
    public boolean canBreakLine(float lineWidth, float maxLineWidth) {
        if (mPenalty == INFINITY) {
            return false;
        } else if (mPenalty == -INFINITY) {
            return true;
        } else {
            // Only if we can fit on the line (e.g., the hyphen).
            return lineWidth + mWidth <= maxLineWidth;
        }
    }
}

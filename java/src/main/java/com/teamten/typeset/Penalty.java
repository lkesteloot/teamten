
package com.teamten.typeset;

/**
 * A location where the line can be broken, but with a penalty.
 */
public class Penalty extends Element {
    public static final int INFINITY = 1000;
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
    public boolean canBreakLine() {
        return mPenalty < INFINITY;
    }
}

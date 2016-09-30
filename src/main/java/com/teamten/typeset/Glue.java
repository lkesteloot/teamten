
package com.teamten.typeset;

/**
 * Whitespace that has a default width but can be shrunk or stretched.
 */
public class Glue extends Element {
    private final float mWidth;
    private final Element mPreviousElement;

    public Glue(float width, Element previousElement) {
        mWidth = width;
        mPreviousElement = previousElement;
    }

    public float getWidth() {
        return mWidth;
    }

    @Override
    public boolean canBreakLine(float lineWidth, float maxLineWidth) {
        return mPreviousElement instanceof Box;
    }
}

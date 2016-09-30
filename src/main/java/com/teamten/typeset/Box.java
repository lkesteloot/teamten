
package com.teamten.typeset;

/**
 * Represents a 2D box of something, like a word or an image.
 */
public class Box extends Element {
    private final float mWidth;
    private final String mText;

    public Box(float width, String text) {
        mWidth = width;
        mText = text;
    }

    public float getWidth() {
        return mWidth;
    }

    public String getText() {
        return mText;
    }

    @Override
    public boolean canBreakLine(float lineWidth, float maxLineWidth) {
        return false;
    }
}

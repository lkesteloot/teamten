package com.teamten.typeset;

import org.jetbrains.annotations.NotNull;

/**
 * The combination of a font and a size.
 */
public class FontSize {
    private final @NotNull Font mFont;
    private final double mSize;

    public FontSize(Font font, double size) {
        mFont = font;
        mSize = size;
    }

    /**
     * The font.
     */
    public Font getFont() {
        return mFont;
    }

    /**
     * The size in points.
     */
    public double getSize() {
        return mSize;
    }
}

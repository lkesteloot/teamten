package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * The combination of a font and a size. Provides some {@link Font}-like methods that omit the font size,
 * since it's already part of this object's state.
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

    /**
     * Get the kerning between the two code points. The result is in scaled points.
     */
    public long getKerning(int leftChar, int rightChar) {
        return mFont.getKerning(leftChar, rightChar, mSize);
    }

    /**
     * The width of a space in scaled points.
     */
    public long getSpaceWidth() {
        return (long) (mFont.getSpaceWidth() * mSize + 0.5);
    }

    /**
     * Return the size of a code point in the specified font size.
     */
    public Font.Metrics getCharacterMetrics(int ch) {
        return mFont.getCharacterMetrics(ch, mSize);
    }

    /**
     * Draw the text.
     * @param text the text to draw.
     * @param x the left-hand edge of the text in scaled points.
     * @param y the baseline of the text in scaled points.
     * @param contents the stream to write to.
     */
    public void draw(String text, long x, long y, PDPageContentStream contents) throws IOException {
        mFont.draw(text, mSize, x, y, contents);
    }

    /**
     * Get the size of the text in the specified font size. Does not include kerning.
     */
    public Font.Metrics getStringMetrics(String text) {
        return mFont.getStringMetrics(text, mSize);
    }
}

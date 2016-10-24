package com.teamten.font;

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
     * Create a new object, replacing the font.
     */
    public FontSize withFont(Font font) {
        return new FontSize(font, mSize);
    }

    /**
     * Get the kerning between the two code points. The result is in scaled points.
     */
    public long getKerning(int leftChar, int rightChar) {
        return mFont.getKerning(leftChar, rightChar, mSize);
    }

    /**
     * Replaces all ligatures (supported by this font) in the string.
     */
    public String transformLigatures(String text) {
        return mFont.transformLigatures(text);
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

    @Override
    public String toString() {
        return String.format("%s %.0fpt", mFont, mSize);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FontSize fontSize = (FontSize) o;

        if (Double.compare(fontSize.mSize, mSize) != 0) return false;
        return mFont.equals(fontSize.mFont);

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = mFont.hashCode();
        temp = Double.doubleToLongBits(mSize);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }
}

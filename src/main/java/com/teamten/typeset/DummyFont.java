package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * A font for testing.
 */
public class DummyFont extends AbstractFont {
    private final long mWidth;
    private final long mHeight;
    private final long mDepth;

    /**
     * Dummy font with reasonable sizes.
     * @param ligatures the ligature list, or null for none.
     */
    public DummyFont(Ligatures ligatures) {
        this(ligatures, PT.toSp(0.5), PT.toSp(0.8), PT.toSp(0.2));
    }

    /**
     * @param ligatures the ligature list, or null for none.
     * @param width width of every character for a 1pt font.
     * @param height height of every character for a 1pt font.
     * @param depth depth of every character for a 1pt font.
     */
    public DummyFont(Ligatures ligatures, long width, long height, long depth) {
        super(ligatures);
        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }

    @Override
    public long getSpaceWidth() {
        return mWidth;
    }

    @Override
    public Metrics getCharacterMetrics(int ch, double fontSize) {
        return new Metrics((long) (mWidth*fontSize), (long) (mHeight*fontSize), (long) (mDepth*fontSize));
    }

    @Override
    public void draw(String text, double fontSize, long x, long y, PDPageContentStream contents) throws IOException {
        // Don't draw.
    }

    @Override
    public String toString() {
        return "DummyFont";
    }
}

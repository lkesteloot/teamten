package com.teamten.typeset;

import com.teamten.util.CodePoints;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * A font that wraps another font and adds tracking (inter-letter spacing). The tracking is defined in
 * units of the font size. Tracking of 0.1 on an 11pt font, for example, would add 1.1pt of space after
 * each letter.
 *
 * <p>Note that the space is added after each letter, not between letters, because the font abstraction does
 * not know where the string of letter ends. This will cause right-aligned text to have a bit of space between
 * it and the margin, and centered text to be slightly to the left of where it should be. You can fix this
 * by using the underlying font for the last letter, at the cost of proper kerning and hyphenation.
 *
 * <p>The font does not support ligatures, and kerning is optionally respected.
 */
public class TrackingFont extends AbstractFont {
    private final Font mUnderlyingFont;
    private final double mTracking;
    private final double mKerning;
    private final long mSpaceWidth;

    /***
     * @param underlyingFont the font to draw.
     * @param tracking fraction of font size to space out characters.
     * @param kerning how much to kern, from 0.0 (no kerning) to 1.0 (full kerning). A value of 0.5 is recommended.
     */
    public TrackingFont(Font underlyingFont, double tracking, double kerning) {
        // No ligatures.
        super();

        mUnderlyingFont = underlyingFont;
        mTracking = tracking;
        mKerning = kerning;
        mSpaceWidth = mUnderlyingFont.getSpaceWidth() + PT.toSp(mTracking);
    }

    @Override
    public long getKerning(int leftChar, int rightChar, double fontSize) {
        if (mKerning != 0.0) {
            // Respect regular kerning.
            return (long) (mUnderlyingFont.getKerning(leftChar, rightChar, fontSize)*mKerning + 0.5);
        } else {
            return 0;
        }
    }

    @Override
    public long getSpaceWidth() {
        return mSpaceWidth;
    }

    @Override
    public Metrics getCharacterMetrics(int ch, float fontSize) {
        Metrics metrics = mUnderlyingFont.getCharacterMetrics(ch, fontSize);

        // Figure out extra space.
        long space = PT.toSp(fontSize*mTracking);

        return new Metrics(metrics.getWidth() + space, metrics.getHeight(), metrics.getDepth());
    }

    @Override
    public void draw(String text, float fontSize, long x, long y, PDPageContentStream contents) throws IOException {
        // Draw one character at a time.
        int i = 0;
        while (i < text.length()) {
            int ch = text.codePointAt(i);

            // Draw the individual character.
            mUnderlyingFont.draw(CodePoints.toString(ch), fontSize, x, y, contents);
            x += getCharacterMetrics(ch, fontSize).getWidth();

            // Next code point.
            i += Character.charCount(ch);
        }
    }
}

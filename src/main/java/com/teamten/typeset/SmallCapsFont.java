package com.teamten.typeset;

import com.teamten.util.CodePoints;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Wraps another font, converting it to small caps by shrinking the size of lower-case letters.
 */
public class SmallCapsFont extends AbstractFont {
    private final Font mUnderlyingFont;
    private final double mSmallSize;

    /***
     * @param underlyingFont the font to draw.
     * @param smallSize fraction of font size to use for the small capitals. A value of 0.8 is recommended.
     */
    public SmallCapsFont(Font underlyingFont, double smallSize) {
        // No ligatures. We could handle them for those that are entirely of the same class (all lower case or all
        // not lower case).
        super();

        mUnderlyingFont = underlyingFont;
        mSmallSize = smallSize;
    }

    @Override
    public long getKerning(int leftChar, int rightChar, double fontSize) {
        // Convert each to fake small caps.
        double leftFontSize = fontSize;
        if (isSmallCapCharacter(leftChar)) {
            leftFontSize *= mSmallSize;
            leftChar = Character.toUpperCase(leftChar);
        }
        double rightFontSize = fontSize;
        if (isSmallCapCharacter(rightChar)) {
            rightFontSize *= mSmallSize;
            rightChar = Character.toUpperCase(rightChar);
        }

        // Average them. This will be correct if they're in the same class. Otherwise it'll get a font size
        // half way, which is probably close.
        double effectiveFontSize = (leftFontSize + rightFontSize) / 2.0;

        return mUnderlyingFont.getKerning(leftChar, rightChar, effectiveFontSize);
    }

    @Override
    public long getSpaceWidth() {
        return mUnderlyingFont.getSpaceWidth();
    }

    @Override
    public Metrics getCharacterMetrics(int ch, double fontSize) {
        if (isSmallCapCharacter(ch)) {
            fontSize *= mSmallSize;
            ch = Character.toUpperCase(ch);
        }

        return mUnderlyingFont.getCharacterMetrics(ch, fontSize);
    }

    @Override
    public void draw(String text, double fontSize, long x, long y, PDPageContentStream contents) throws IOException {
        // Draw one character at a time. We could be more clever here and find spans of characters of the same class,
        // but very little of the book is in small caps, so it would buy us little in performance or output file size.
        int i = 0;
        while (i < text.length()) {
            int ch = text.codePointAt(i);

            // Convert to fake small caps.
            double characterFontSize = fontSize;
            if (isSmallCapCharacter(ch)) {
                characterFontSize *= mSmallSize;
                ch = Character.toUpperCase(ch);
            }

            // Draw the individual character.
            mUnderlyingFont.draw(CodePoints.toString(ch), characterFontSize, x, y, contents);
            x += getCharacterMetrics(ch, characterFontSize).getWidth();

            // Next code point.
            i += Character.charCount(ch);
        }
    }

    /**
     * Whether the code point should be drawn in small caps.
     */
    private boolean isSmallCapCharacter(int ch) {
        // Make all lower case letters small caps.
        return Character.isLowerCase(ch);
    }
}

package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * A sequence of characters.
 */
public class Text extends Box {
    private final Font mFont;
    private final float mFontSize;
    private final String mText;

    public Text(Font font, float fontSize, String text, long width, long height, long depth) {
        super(width, height, depth);
        mFont = font;
        mFontSize = fontSize;
        mText = text;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        contents.beginText();
        contents.setFont(mFont.getPdFont(), mFontSize);
        contents.newLineAtOffset(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y));
        contents.showText(mText);
        contents.endText();

        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Text must always be in an HBox.
        throw new IllegalStateException("text should be not laid out vertically");
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%sText %s: “%s” in %.0fpt %s%n", indent, getDimensionString(), mText, mFontSize, mFont);
    }
}
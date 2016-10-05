package com.teamten.typeset;

import com.teamten.util.CodePoints;
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

    /**
     * Constructor for string of any size.
     */
    public Text(Font font, float fontSize, String text, long width, long height, long depth) {
        super(width, height, depth);
        mFont = font;
        mFontSize = fontSize;
        mText = text;
    }

    /**
     * Constructor for single character.
     */
    public Text(int ch, Font font, float fontSize) throws IOException {
        super(getTextDimensions(ch, font, fontSize));
        mFont = font;
        mFontSize = fontSize;
        mText = CodePoints.toString(ch);
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        /// drawDebugRectangle(contents, x, y);

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

    /**
     * Get the dimensions of a character.
     */
    private static BoxDimensions getTextDimensions(int ch, Font font, float fontSize) throws IOException {
        Font.Metrics metrics = font.getCharacterMetrics(ch, fontSize);
        return new BoxDimensions(metrics.getWidth(), metrics.getHeight(), metrics.getDepth());
    }
}

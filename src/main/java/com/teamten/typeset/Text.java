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
     * Constructor for a string.
     */
    public Text(String text, Font font, float fontSize) throws IOException {
        super(getTextDimensions(text, font, fontSize));
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

    /**
     * The text that this element was constructed with.
     */
    public String getText() {
        return mText;
    }

    /**
     * The font the text should be displayed in.
     */
    public Font getFont() {
        return mFont;
    }

    /**
     * The font size in points.
     */
    public float getFontSize() {
        return mFontSize;
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

    @Override
    public String toTextString() {
        return mText;
    }

    /**
     * Get the dimensions of a string.
     */
    private static AbstractDimensions getTextDimensions(String text, Font font, float fontSize) throws IOException {
        Font.Metrics metrics = font.getStringMetrics(text, fontSize);
        return new AbstractDimensions(metrics.getWidth(), metrics.getHeight(), metrics.getDepth());
    }

    /**
     * Get the dimensions of a character.
     */
    private static AbstractDimensions getTextDimensions(int ch, Font font, float fontSize) throws IOException {
        Font.Metrics metrics = font.getCharacterMetrics(ch, fontSize);
        return new AbstractDimensions(metrics.getWidth(), metrics.getHeight(), metrics.getDepth());
    }
}

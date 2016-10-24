package com.teamten.typeset;

import com.google.common.math.DoubleMath;
import com.teamten.font.FontSize;
import com.teamten.util.CodePoints;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PrintStream;

/**
 * A sequence of characters.
 */
public class Text extends Box {
    private final @NotNull FontSize mFont;
    private final @NotNull String mText;

    /**
     * Constructor for string of any size.
     */
    public Text(FontSize font, String text, long width, long height, long depth) {
        super(width, height, depth);
        mFont = font;
        mText = text;
    }

    /**
     * Constructor for a string.
     */
    public Text(String text, FontSize font) {
        super(font.getStringMetrics(text));
        mFont = font;
        mText = text;
    }

    /**
     * Constructor for single character.
     */
    public Text(int ch, FontSize font) {
        super(font.getCharacterMetrics(ch));
        mFont = font;
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
    public FontSize getFont() {
        return mFont;
    }

    /**
     * Whether this text can be appended to the other text.
     */
    public boolean isCompatibleWith(Text other) {
        return mFont.getFont() == other.mFont.getFont() &&
                DoubleMath.fuzzyEquals(mFont.getSize(), other.mFont.getSize(), 0.001);
    }

    /**
     * Returns a new Text object, the text of which is the concatenation of this text and
     * the other text.
     *
     * @throws IllegalArgumentException if the two text objects are not compatible.
     */
    public Text appendedWith(Text other) {
        if (!isCompatibleWith(other)) {
            throw new IllegalArgumentException("incompatible text, cannot append");
        }

        return new Text(mText + other.mText, mFont);
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        /// drawDebugRectangle(contents, x, y);

        mFont.draw(mText, x, y, contents);

        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Text must always be in an HBox.
        throw new IllegalStateException("text should be not laid out vertically");
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.print(indent);
        stream.println(toString());
    }

    @Override
    public String toString() {
        return String.format("Text %s: “%s” in %s", getDimensionString(), mText, mFont);
    }

    @Override
    public String toTextString() {
        return mText;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Text text = (Text) o;

        if (!mFont.equals(text.mFont)) return false;
        return mText.equals(text.mText);

    }

    @Override
    public int hashCode() {
        int result = mFont.hashCode();
        result = 31 * result + mText.hashCode();
        return result;
    }
}

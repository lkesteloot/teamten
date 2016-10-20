package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Like glue, but draws a pattern over and over (e.g., for table of contents).
 */
public class Leader extends Glue {
    private final Font mFont;
    private final float mFontSize;
    private final String mPattern;
    private final long mPatternWidth;

    private Leader(Font font, float fontSize, String pattern, long size, long stretch) {
        super(size, stretch, stretch != 0, 0, false, true);
        mFont = font;
        mFontSize = fontSize;
        mPattern = pattern;
        mPatternWidth = mFont.getStringMetrics(mPattern, mFontSize).getWidth();
    }

    /**
     * Creates a leader which can be used like any other horizontal glue.
     *
     * @param pattern For table of contents you want something like " . "
     * @param stretch how much (infinite) stretchability. 1pt is good for this.
     */
    public Leader(Font font, float fontSize, String pattern, long stretch) {
        this(font, fontSize, pattern, 0, stretch);
    }

    @Override
    public Glue fixed(long newSize) {
        return new Leader(mFont, mFontSize, mPattern, newSize, 0);
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        // Align the dots to the left size of the page. TeX does it to the left of the enclosing box,
        // but we don't have that and it doesn't matter anyway.
        long startX = (x + mPatternWidth - 1) / mPatternWidth * mPatternWidth;
        long endX = x + getSize() - mPatternWidth;

        // Draw the pattern all along the glue length.
        for (long position = startX; position < endX; position += mPatternWidth) {
            contents.beginText();
            contents.setFont(((PdfBoxFont) mFont).getPdFont(), mFontSize);
            contents.newLineAtOffset(PT.fromSpAsFloat(position), PT.fromSpAsFloat(y));
            contents.showText(mPattern);
            contents.endText();
        }

        return getSize();
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%sLeader: %.1fpt%s, pattern \"%s\"%n", indent,
                PT.fromSp(getSize()), getStretch().toString("+"), mPattern);
    }

    @Override
    public String toTextString() {
        return " . . . ";
    }
}

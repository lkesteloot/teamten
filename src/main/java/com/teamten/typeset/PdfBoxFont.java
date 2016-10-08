
package com.teamten.typeset;

import org.apache.commons.io.FilenameUtils;
import org.apache.fontbox.ttf.CmapSubtable;
import org.apache.fontbox.ttf.CmapTable;
import org.apache.fontbox.ttf.GlyphData;
import org.apache.fontbox.ttf.GlyphTable;
import org.apache.fontbox.ttf.KerningSubtable;
import org.apache.fontbox.ttf.KerningTable;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.File;
import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Wrapper around a PDFBOX font.
 */
public class PdfBoxFont extends AbstractFont {
    private static final TTFParser TTF_PARSER = new TTFParser(true);
    private final File mFile;
    private final PDFont mPdFont;
    private final KerningSubtable mKerningSubtable;
    private final CmapSubtable mCmapSubtable;
    private final GlyphTable mGlyphTable;
    private final int mUnitsPerEm;
    private final long mSpaceWidth;

    public PdfBoxFont(PDDocument pdf, File file) throws IOException {
        mFile = file;

        // Load the font.
        TrueTypeFont ttf = TTF_PARSER.parse(file);
        mPdFont = PDType0Font.load(pdf, ttf, false);

        // Load the kerning table.
        KerningTable kerningTable = ttf.getKerning();
        if (kerningTable == null) {
            mKerningSubtable = null;
        } else {
            mKerningSubtable = kerningTable.getHorizontalKerningSubtable();
        }

        // Load the map from Unicode to glyph ID. We need the glyph ID when looking up the kerning info.
        CmapTable cmap = ttf.getCmap();
        mCmapSubtable = cmap.getSubtable(CmapTable.PLATFORM_UNICODE, CmapTable.ENCODING_UNICODE_2_0_BMP);

        // The kerning numbers are given in these units.
        mUnitsPerEm = ttf.getUnitsPerEm();

        // Make a table of ligatures that only include the ones in this font.
        setLigatures(new Ligatures(ch -> mCmapSubtable.getGlyphId(ch) != 0));

        // Get the table of glyphs that'll give us the metrics for each glyph.
        mGlyphTable = ttf.getGlyph();

        // Cache the width of a space for a 1pt font.
        mSpaceWidth = getCharacterMetrics(' ', 1.0f).getWidth();
    }

    /**
     * The PDFont that this font is based on.
     */
    public PDFont getPdFont() {
        return mPdFont;
    }

    /**
     * Get the kerning between the two code points. The result is in scaled points.
     *
     * @param fontSize the size of the font in points.
     */
    @Override
    public long getKerning(int leftChar, int rightChar, double fontSize) {
        int leftGlyph = mCmapSubtable.getGlyphId(leftChar);
        int rightGlyph = mCmapSubtable.getGlyphId(rightChar);

        return PT.toSp(mKerningSubtable.getKerning(leftGlyph, rightGlyph) * fontSize / mUnitsPerEm);
    }

    /**
     * The width of a space for a 1pt font, in scaled points.
     */
    @Override
    public long getSpaceWidth() {
        return mSpaceWidth;
    }

    /**
     * The basename ("Times New Roman") of the font filename.
     */
    @Override
    public String toString() {
        return FilenameUtils.getBaseName(mFile.getName());
    }

    /**
     * Return the size of a code point in the specified font size.
     */
    @Override
    public Metrics getCharacterMetrics(int ch, float fontSize) throws IOException {
        int glyphId = mCmapSubtable.getGlyphId(ch);

        // Width we can get directly.
        long width = PT.toSp(mPdFont.getWidth(glyphId) / 1000 * fontSize);

        // Height and depth we get from the glyph data.
        GlyphData glyphData = mGlyphTable.getGlyph(glyphId);

        long height;
        long depth;
        if (glyphData == null) {
            // No glyph, probably a space.
            height = 0;
            depth = 0;
        } else {
            height = Math.max(PT.toSp(glyphData.getYMaximum() * fontSize / mUnitsPerEm), 0);
            depth = Math.max(-PT.toSp(glyphData.getYMinimum() * fontSize / mUnitsPerEm), 0);
        }

        return new Metrics(width, height, depth);
    }
}

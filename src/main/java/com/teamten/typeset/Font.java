
package com.teamten.typeset;

import org.apache.fontbox.ttf.CmapSubtable;
import org.apache.fontbox.ttf.CmapTable;
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
public class Font {
    private static final TTFParser TTF_PARSER = new TTFParser(true);
    private final PDFont mPdFont;
    private final KerningSubtable mKerningSubtable;
    private final CmapSubtable mCmapSubtable;
    private final int mUnitsPerEm;
    private final Ligatures mLigatures;

    public Font(PDDocument pdf, File file) throws IOException {
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
        mLigatures = new Ligatures(ch -> mCmapSubtable.getGlyphId(ch) != 0);
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
    public long getKerning(int leftChar, int rightChar, double fontSize) {
        int leftGlyph = mCmapSubtable.getGlyphId(leftChar);
        int rightGlyph = mCmapSubtable.getGlyphId(rightChar);

        return PT.toSp(mKerningSubtable.getKerning(leftGlyph, rightGlyph) * fontSize / mUnitsPerEm);
    }

    public String transformLigatures(String s) {
        return mLigatures.transform(s);
    }
}

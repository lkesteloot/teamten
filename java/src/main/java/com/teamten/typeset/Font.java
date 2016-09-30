
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
        TrueTypeFont ttf = TTF_PARSER.parse(file);
        mPdFont = PDType0Font.load(pdf, ttf, false);
        KerningTable kerningTable = ttf.getKerning();
        if (kerningTable == null) {
            mKerningSubtable = null;
        } else {
            mKerningSubtable = kerningTable.getHorizontalKerningSubtable();
        }

        CmapTable cmap = ttf.getCmap();
        mCmapSubtable = cmap.getSubtable(CmapTable.PLATFORM_UNICODE, CmapTable.ENCODING_UNICODE_2_0_BMP);

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
     * Get the kerning between the two characters. The result is in the same units.
     * as PDFont.getStringWidth().
     */
    public float getKerning(char leftChar, char rightChar) {
        int leftGlyph = mCmapSubtable.getGlyphId(leftChar);
        int rightGlyph = mCmapSubtable.getGlyphId(rightChar);
        return mKerningSubtable.getKerning(leftGlyph, rightGlyph) * 1000f / mUnitsPerEm;
    }

    public String transformLigatures(String s) {
        return mLigatures.transform(s);
    }
}

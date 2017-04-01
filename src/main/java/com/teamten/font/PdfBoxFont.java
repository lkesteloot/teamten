/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.font;

import com.teamten.typeset.Ligatures;
import org.apache.commons.io.FilenameUtils;
import org.apache.fontbox.ttf.CmapSubtable;
import org.apache.fontbox.ttf.CmapTable;
import org.apache.fontbox.ttf.GlyphData;
import org.apache.fontbox.ttf.GlyphTable;
import org.apache.fontbox.ttf.KerningSubtable;
import org.apache.fontbox.ttf.KerningTable;
import org.apache.fontbox.ttf.OTFParser;
import org.apache.fontbox.ttf.TTFParser;
import org.apache.fontbox.ttf.TrueTypeFont;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionGoTo;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageFitWidthDestination;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Wrapper around a PDFBOX font.
 */
public class PdfBoxFont extends AbstractFont {
    private static final TTFParser TTF_PARSER = new TTFParser(true);
    private static final OTFParser OTF_PARSER = new OTFParser(true);
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
        TrueTypeFont ttf;
        if (file.getName().toLowerCase().endsWith(".otf")) {
            // OTF support is untested. The one font I tried threw an exception because the "loca" table
            // wasn't found.
            ttf = OTF_PARSER.parse(file);
        } else {
            ttf = TTF_PARSER.parse(file);
        }
        mPdFont = PDType0Font.load(pdf, ttf, true);

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
        if (mKerningSubtable == null) {
            return 0;
        } else {
            int leftGlyph = mCmapSubtable.getGlyphId(leftChar);
            int rightGlyph = mCmapSubtable.getGlyphId(rightChar);

            return PT.toSp(mKerningSubtable.getKerning(leftGlyph, rightGlyph)*fontSize/mUnitsPerEm);
        }
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
    public Metrics getCharacterMetrics(int ch, double fontSize) {
        try {
            int glyphId = mCmapSubtable.getGlyphId(ch);

            // Width we can get directly.
            long width = PT.toSp(mPdFont.getWidth(glyphId)/1000*fontSize);

            // Height and depth we get from the glyph data.
            GlyphData glyphData = mGlyphTable.getGlyph(glyphId);

            long height;
            long depth;
            if (glyphData == null) {
                // No glyph, probably a space.
                height = 0;
                depth = 0;
            } else {
                height = Math.max(PT.toSp(glyphData.getYMaximum()*fontSize/mUnitsPerEm), 0);
                depth = Math.max(-PT.toSp(glyphData.getYMinimum()*fontSize/mUnitsPerEm), 0);
            }

            return new Metrics(width, height, depth);
        } catch (IOException e) {
            // Normally I don't like to convert checked exceptions to unchecked, but in this case I think
            // it's not possible for this to happen, and forcing a checked exception on this method causes
            // disruption all the way up the chain.
            throw new UncheckedIOException("got an exception getting the character metrics", e);
        }
    }

    @Override
    public void draw(String text, double fontSize, long x, long y, PDPageContentStream contents) throws IOException {
        // Here some non-break spaces might get through. Replace them because they don't have a glyph.
        text = text.replaceAll("\u202F", " ").replaceAll("\u00A0", " ");

        contents.beginText();
        contents.setFont(getPdFont(), (float) fontSize);
        contents.newLineAtOffset(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y));
        contents.showText(text);
        contents.endText();
        PDPageDestination destination = new PDPageFitDestination(); // TODO maybe XYZ?
        destination.setPageNumber(123);
        PDAnnotationLink link = new PDAnnotationLink();
        link.setDestination(destination);
        Metrics metrics = getStringMetrics(text, fontSize);
        PDRectangle rectangle = new PDRectangle(
                PT.fromSpAsFloat(x),
                PT.fromSpAsFloat(y),
                PT.fromSpAsFloat(metrics.getWidth()),
                PT.fromSpAsFloat(metrics.getHeight() + metrics.getDepth()));
        link.setRectangle(rectangle);
        PDPage page;
        page.getAnnotations().add(link);
    }
}

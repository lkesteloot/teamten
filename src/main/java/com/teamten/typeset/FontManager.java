package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads and manages fonts.
 */
public class FontManager {
    private static final File FONT_DIR = new File("/Library/Fonts");
    private static final File SYSTEM_FONT_DIR = new File("/System/Library/Fonts");
    private static final File MY_FONT_DIR = new File("/Users/lk/Dropbox/Personal/Fonts");
    private final PDDocument mPdDoc;
    private final Map<FontName,Font> mFontMap = new HashMap<>();

    /**
     * Represents the individual fonts that we know about.
     */
    public enum FontName {
        TIMES_NEW_ROMAN(FONT_DIR, "Times New Roman.ttf"),
        TIMES_NEW_ROMAN_BOLD(FONT_DIR, "Times New Roman Bold.ttf"),
        TIMES_NEW_ROMAN_ITALIC(FONT_DIR, "Times New Roman Italic.ttf"),
        TIMES_NEW_ROMAN_BOLD_ITALIC(FONT_DIR, "Times New Roman Italic.ttf"),
        MINION(MY_FONT_DIR, "Minion/MinionPro-Regular.ttf"),
        MINION_BOLD(FONT_DIR, "MinionPro-Bold.otf"),
        MINION_ITALIC(MY_FONT_DIR, "Minion/MinionPro-It.ttf"),
        MINION_BOLD_ITALIC(FONT_DIR, "MinionPro-BoldIt.otf"),
        MINION_SMALL_CAPS(MY_FONT_DIR, "Minion/Minion Small Caps  Oldstyle Fi Regular.ttf"),
        HELVETICA_NEUE_ULTRA_LIGHT(MY_FONT_DIR, "Helvetica Neue/HelveticaNeue-UltraLight.ttf");

        private final File mDirectory;
        private final String mFilename;

        FontName(File directory, String filename) {
            mDirectory = directory;
            mFilename = filename;
        }

        public File getFile() {
            return new File(mDirectory, mFilename);
        }
    }

    public FontManager(PDDocument pdDoc) {
        mPdDoc = pdDoc;
    }

    /**
     * Fetches a font. The first time this is called for a particular font, the font is loaded.
     *
     * @throws IOException if the font cannot be loaded.
     */
    public Font get(FontName fontName) throws IOException {
        synchronized (mFontMap) {
            Font font = mFontMap.get(fontName);
            if (font == null) {
                font = new PdfBoxFont(mPdDoc, fontName.getFile());
                mFontMap.put(fontName, font);
            }

            return font;
        }
    }
}

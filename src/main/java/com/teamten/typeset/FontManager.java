package com.teamten.typeset;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads and manages fonts.
 */
public class FontManager {
    private final Function<File,Font> mFontLoader;
    private final Map<FontName,Font> mFontMap = new HashMap<>();

    /**
     * A font manager that creates new fonts from the specified font loader. The font loader should
     * throw an IllegalArgumentException if the font cannot be loaded.
     */
    public FontManager(Function<File,Font> fontLoader) {
        mFontLoader = fontLoader;
    }

    /**
     * Fetches a font. The first time this is called for a particular font, the font is loaded.
     *
     * @throws IllegalArgumentException if the font cannot be loaded.
     */
    public Font get(FontName fontName) {
        synchronized (mFontMap) {
            Font font = mFontMap.get(fontName);
            if (font == null) {
                font = mFontLoader.apply(fontName.getFile());
                mFontMap.put(fontName, font);
            }

            return font;
        }
    }
}

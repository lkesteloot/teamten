package com.teamten.typeset;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Loads and manages fonts.
 */
public class FontManager {
    private final Function<TypefaceVariant,Font> mFontLoader;
    private final Map<TypefaceVariant,Font> mFontCache = new HashMap<>();

    /**
     * A font manager that creates new fonts from the specified font loader. The font loader should
     * throw an IllegalArgumentException if the font cannot be loaded.
     */
    public FontManager(Function<TypefaceVariant,Font> fontLoader) {
        mFontLoader = fontLoader;
    }

    /**
     * Fetches a font. The first time this is called for a particular font, the font is loaded.
     *
     * @throws IllegalArgumentException if the font cannot be loaded.
     */
    public Font get(TypefaceVariant typefaceVariant) {
        synchronized (mFontCache) {
            Font font = mFontCache.get(typefaceVariant);
            if (font == null) {
                font = mFontLoader.apply(typefaceVariant);
                mFontCache.put(typefaceVariant, font);
            }

            return font;
        }
    }

    /**
     * Fetches a font, returning the font and size together.
     *
     * @throws IllegalArgumentException if the font cannot be loaded.
     */
    public FontSize get(TypefaceVariantSize typefaceVariantSize) {
        Font font = get((TypefaceVariant) typefaceVariantSize);

        return new FontSize(font, typefaceVariantSize.getSize());
    }

    /**
     * Utility method that calls {@link #get(TypefaceVariant)} with a new {@link TypefaceVariant} object
     * created from the two parameters.
     *
     * TODO can eventually delete.
     */
    public Font get(Typeface typeface, FontVariant fontVariant) {
        return get(new TypefaceVariant(typeface, fontVariant));
    }
}

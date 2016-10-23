package com.teamten.typeset;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Tracks sets of fonts for a given typeface.
 */
public enum Typeface {
    TIMES_NEW_ROMAN(new MapBuilder()
            .with(FontVariant.REGULAR, FontName.TIMES_NEW_ROMAN)
            .with(FontVariant.BOLD, FontName.TIMES_NEW_ROMAN_BOLD)
            .with(FontVariant.ITALIC, FontName.TIMES_NEW_ROMAN_ITALIC)
            .with(FontVariant.BOLD_ITALIC, FontName.TIMES_NEW_ROMAN_BOLD_ITALIC)
            .build()),
    MINION(new MapBuilder()
            .with(FontVariant.REGULAR, FontName.MINION)
            .with(FontVariant.BOLD, FontName.MINION_BOLD)
            .with(FontVariant.ITALIC, FontName.MINION_ITALIC)
            .with(FontVariant.BOLD_ITALIC, FontName.MINION_BOLD_ITALIC)
            .with(FontVariant.SMALL_CAPS, FontName.MINION_SMALL_CAPS)
            .build());

    private final Map<FontVariant,FontName> mFontMap;

    Typeface(Map<FontVariant,FontName> fontMap) {
        mFontMap = fontMap;
    }

    /**
     * Return the font name for this variant, or null if the typeface does not define one.
     */
    FontName get(FontVariant fontVariant) {
        return mFontMap.get(fontVariant);
    }

    /**
     * Utility class to help build the map of font variants.
     */
    private static class MapBuilder {
        private final Map<FontVariant,FontName> mFontMap = new HashMap<>();

        /**
         * Fluid method to add an entry to the map.
         */
        MapBuilder with(FontVariant fontVariant, FontName fontName) {
            mFontMap.put(fontVariant, fontName);
            return this;
        }

        /**
         * Returns an unmodifiable version of the map.
         */
        Map<FontVariant,FontName> build() {
            return Collections.unmodifiableMap(mFontMap);
        }
    }

    /**
     * Parses a typeface name by converting it to upper case and transforming spaces and hyphens to
     * underscores, then looking it up in this enum.
     *
     * @throws IllegalArgumentException if the typeface is not found.
     */
    public static Typeface parse(String s) {
        return valueOf(s.toUpperCase().replace(' ', '_').replace('-', '_'));
    }
}

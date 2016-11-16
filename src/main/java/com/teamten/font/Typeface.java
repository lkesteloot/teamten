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
            .build()),
    /**
     * https://fonts.google.com/specimen/Alegreya
     */
    ALEGREYA(new MapBuilder()
            .with(FontVariant.REGULAR, FontName.ALEGREYA_REGULAR)
            .with(FontVariant.ITALIC, FontName.ALEGREYA_ITALIC)
            .with(FontVariant.SMALL_CAPS, FontName.ALEGREYA_SMALL_CAPS)
            .build()),
    /**
     * https://fonts.google.com/specimen/IM+Fell+English
     */
    IM_FELL_ENGLISH(new MapBuilder()
            .with(FontVariant.REGULAR, FontName.IM_FELL_ENGLISH_REGULAR)
            .with(FontVariant.ITALIC, FontName.IM_FELL_ENGLISH_ITALIC)
            .with(FontVariant.SMALL_CAPS, FontName.IM_FELL_ENGLISH_SMALL_CAPS)
            .build()),
    /**
     * https://fonts.google.com/specimen/Sorts+Mill+Goudy
     */
    SORTS_MILL_GOUDY(new MapBuilder()
            .with(FontVariant.REGULAR, FontName.SORTS_MILL_GOUDY_REGULAR)
            .with(FontVariant.ITALIC, FontName.SORTS_MILL_GOUDY_ITALIC)
            .build());

    private final Map<FontVariant,FontName> mFontMap;

    Typeface(Map<FontVariant,FontName> fontMap) {
        mFontMap = fontMap;
    }

    /**
     * Return the font name for this variant, or null if the typeface does not define one.
     */
    public FontName get(FontVariant fontVariant) {
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

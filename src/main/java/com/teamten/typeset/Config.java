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

package com.teamten.typeset;

import com.teamten.font.TypefaceVariantSize;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores all the configuration metadata about the book. This includes strings like the title of the
 * book, font descriptions, and page sizes.
 */
public class Config {
    /**
     * See {@link KeyType} for valid values.
     */
    private final Map<Key,Object> mMetadata = new HashMap<>();

    /**
     * Keys are mapped to different kinds of values, and this enum represents those various types.
     */
    private enum KeyType {
        /**
         * Value is a String.
         */
        STRING,

        /**
         * Value is a TypefaceVariantSize.
         */
        FONT,

        /**
         * Value is a Long in scaled points.
         */
        DISTANCE,
    }

    /**
     * Various keys that can be used in this configuration.
     */
    public enum Key {
        // Strings.
        TITLE(KeyType.STRING),
        AUTHOR(KeyType.STRING),
        PUBLISHER_NAME(KeyType.STRING),
        PUBLISHER_LOCATION(KeyType.STRING),
        COPYRIGHT(KeyType.STRING),
        COLOPHON(KeyType.STRING),
        TOC_TITLE(KeyType.STRING),
        INDEX_TITLE(KeyType.STRING),
        LANGUAGE(KeyType.STRING),

        // Fonts.
        BODY_FONT(KeyType.FONT),
        CAPTION_FONT(KeyType.FONT),
        PART_HEADER_FONT(KeyType.FONT),
        CHAPTER_HEADER_FONT(KeyType.FONT),
        MINOR_HEADER_FONT(KeyType.FONT),
        PAGE_NUMBER_FONT(KeyType.FONT),
        HEADLINE_FONT(KeyType.FONT),
        HALF_TITLE_PAGE_TITLE_FONT(KeyType.FONT),
        TITLE_PAGE_AUTHOR_FONT(KeyType.FONT),
        TITLE_PAGE_TITLE_FONT(KeyType.FONT),
        TITLE_PAGE_PUBLISHER_NAME_FONT(KeyType.FONT),
        TITLE_PAGE_PUBLISHER_LOCATION_FONT(KeyType.FONT),
        COPYRIGHT_PAGE_COPYRIGHT_FONT(KeyType.FONT),
        COPYRIGHT_PAGE_COLOPHON_FONT(KeyType.FONT),
        TOC_PAGE_TITLE_FONT(KeyType.FONT),
        TOC_PAGE_PART_FONT(KeyType.FONT),
        TOC_PAGE_CHAPTER_FONT(KeyType.FONT),

        // Distances
        PAGE_WIDTH(KeyType.DISTANCE),
        PAGE_HEIGHT(KeyType.DISTANCE),
        PAGE_MARGIN_TOP(KeyType.DISTANCE),
        PAGE_MARGIN_BOTTOM(KeyType.DISTANCE),
        PAGE_MARGIN_OUTER(KeyType.DISTANCE),
        PAGE_MARGIN_INNER(KeyType.DISTANCE);

        private final KeyType mKeyType;

        Key(KeyType keyType) {
            mKeyType = keyType;
        }

        /**
         * Specifies what kind of key this is (string, font, or distance).
         */
        public KeyType getKeyType() {
            return mKeyType;
        }

        /**
         * The string key is converted to upper case, and hyphens are converted to underscores.
         *
         * @throws IllegalArgumentException if the key does not match one of the entries in the enum.
         */
        public static Key fromHeader(String stringKey) {
            // Convert to upper case and replace hyphens with underscores.
            stringKey = stringKey.toUpperCase().replace("-", "_");

            // This will throw an IllegalArgumentException if the key is not found in the enum.
            return valueOf(stringKey);
        }
    }

    /**
     * A configuration suitable for quick tests.
     */
    public static Config testConfig() {
        Config config = new Config();

        config.fillWithDefaults();

        return config;
    }

    /**
     * Fill the configuration with default values so that each document doesn't have to define them all.
     */
    public void fillWithDefaults() {
        add(Key.PAGE_WIDTH, "8.5in");
        add(Key.PAGE_HEIGHT, "11in");
        add(Key.PAGE_MARGIN_TOP, "6pc");
        add(Key.PAGE_MARGIN_BOTTOM, "6pc");
        add(Key.PAGE_MARGIN_OUTER, "6pc");
        add(Key.PAGE_MARGIN_INNER, "6pc");
        add(Key.BODY_FONT, "Times New Roman, regular, 11pt");
        add(Key.CAPTION_FONT, "Times New Roman, regular, 11pt");
        add(Key.PART_HEADER_FONT, "Times New Roman, regular, 11pt");
        add(Key.CHAPTER_HEADER_FONT, "Times New Roman, regular, 11pt");
        add(Key.PAGE_NUMBER_FONT, "Times New Roman, regular, 11pt");
        add(Key.HEADLINE_FONT, "Times New Roman, regular, 11pt");
    }

    /**
     * Adds a string value to the configuration.
     *
     * @throws IllegalArgumentException if the key is not for strings.
     */
    public void addString(Key key, String value) {
        if (key.getKeyType() != KeyType.STRING) {
            throw new IllegalArgumentException("key " + key + " is for " + key.getKeyType());
        }

        mMetadata.put(key, value);
    }

    /**
     * Adds a font value to the configuration.
     *
     * @throws IllegalArgumentException if the key is not for fonts.
     */
    public void addFont(Key key, TypefaceVariantSize value) {
        if (key.getKeyType() != KeyType.FONT) {
            throw new IllegalArgumentException("key " + key + " is for " + key.getKeyType());
        }

        mMetadata.put(key, value);
    }

    /**
     * Adds a distance value to the configuration.
     *
     * @throws IllegalArgumentException if the key is not for distances.
     */
    public void addDistance(Key key, long value) {
        if (key.getKeyType() != KeyType.DISTANCE) {
            throw new IllegalArgumentException("key " + key + " is for " + key.getKeyType());
        }

        mMetadata.put(key, value);
    }

    /**
     * Parses the value appropriately for the key type and adds it to the configuration.
     *
     * @throws IllegalArgumentException if the value cannot be parsed for the given type.
     */
    public void add(Key key, String value) {
        switch (key.getKeyType()) {
            case STRING:
                addString(key, value);
                break;

            case FONT:
                addFont(key, TypefaceVariantSize.valueOf(value));
                break;

            case DISTANCE:
                addDistance(key, SpaceUnit.parseDistance(value));
                break;

            default:
                throw new IllegalArgumentException("key " + key + " of unknown type");
        }
    }

    /**
     * Gets the string value for the key, or null if not in the configuration.
     *
     * @throws IllegalArgumentException if the key is not for strings.
     */
    public String getString(Key key) {
        if (key.getKeyType() != KeyType.STRING) {
            throw new IllegalArgumentException("key " + key + " is for " + key.getKeyType());
        }

        return (String) mMetadata.get(key);
    }

    /**
     * Gets the font value for the key, or null if not in the configuration.
     *
     * @throws IllegalArgumentException if the key is not for fonts.
     */
    public TypefaceVariantSize getFont(Key key) {
        if (key.getKeyType() != KeyType.FONT) {
            throw new IllegalArgumentException("key " + key + " is for " + key.getKeyType());
        }

        return (TypefaceVariantSize) mMetadata.get(key);
    }

    /**
     * Gets the distance value for the key in scaled units, or throws if not in the configuration.
     *
     * @throws IllegalArgumentException if the key is not for distances or the value not in the configuration.
     */
    public long getDistance(Key key) {
        if (key.getKeyType() != KeyType.DISTANCE) {
            throw new IllegalArgumentException("key " + key + " is for " + key.getKeyType());
        }

        Long distance = (Long) mMetadata.get(key);
        if (distance == null) {
            throw new IllegalArgumentException("key " + key + " is not in the configuration");
        }

        return distance;
    }

    /**
     * Gets the page width in scaled points from {@link Key#PAGE_WIDTH}.
     */
    public long getPageWidth() {
        return getDistance(Key.PAGE_WIDTH);
    }

    /**
     * Gets the page height in scaled points from {@link Key#PAGE_HEIGHT}.
     */
    public long getPageHeight() {
        return getDistance(Key.PAGE_HEIGHT);
    }

    /**
     * Gets the page top margin in scaled points from {@link Key#PAGE_MARGIN_TOP}.
     */
    public long getPageMarginTop() {
        return getDistance(Key.PAGE_MARGIN_TOP);
    }

    /**
     * Gets the page bottom margin in scaled points from {@link Key#PAGE_MARGIN_BOTTOM}.
     */
    public long getPageMarginBottom() {
        return getDistance(Key.PAGE_MARGIN_BOTTOM);
    }

    /**
     * Gets the page outer margin in scaled points from {@link Key#PAGE_MARGIN_OUTER}.
     */
    public long getPageMarginOuter() {
        return getDistance(Key.PAGE_MARGIN_OUTER);
    }

    /**
     * Gets the page inner margin in scaled points from {@link Key#PAGE_MARGIN_INNER}.
     */
    public long getPageMarginInner() {
        return getDistance(Key.PAGE_MARGIN_INNER);
    }

    /**
     * The width of the text on the page. This is computed from the page width and page margin.
     */
    public long getBodyWidth() {
        return getPageWidth() - getPageMarginOuter() - getPageMarginInner();
    }

    /**
     * The height of the text on the page. This is computed from the page height and page margin.
     */
    public long getBodyHeight() {
        return getPageHeight() - getPageMarginTop() - getPageMarginBottom();
    }
}

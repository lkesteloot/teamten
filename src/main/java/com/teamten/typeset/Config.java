package com.teamten.typeset;

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
        PRINTING(KeyType.STRING),
        TOC_TITLE(KeyType.STRING),
        LANGUAGE(KeyType.STRING),

        // Fonts.
        PAGE_NUMBER_FONT(KeyType.FONT),
        HEADLINE_FONT(KeyType.FONT), // TODO use

        // Distances
        PAGE_WIDTH(KeyType.DISTANCE),
        PAGE_HEIGHT(KeyType.DISTANCE),
        PAGE_MARGIN(KeyType.DISTANCE);

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

        config.add(Config.Key.PAGE_WIDTH, "6in");
        config.add(Config.Key.PAGE_HEIGHT, "9in");
        config.add(Config.Key.PAGE_MARGIN, "1in");
        config.add(Config.Key.PAGE_NUMBER_FONT, "Times New Roman, regular, 11pt");

        return config;
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
     * Gets the page margin in scaled points from {@link Key#PAGE_MARGIN}.
     */
    public long getPageMargin() {
        return getDistance(Key.PAGE_MARGIN);
    }

    /**
     * The width of the text on the page. This is computed from the page width and page margin.
     */
    public long getBodyWidth() {
        return getPageWidth() - 2*getPageMargin();
    }

    /**
     * The height of the text on the page. This is computed from the page height and page margin.
     */
    public long getBodyHeight() {
        return getPageHeight() - 2*getPageMargin();
    }
}

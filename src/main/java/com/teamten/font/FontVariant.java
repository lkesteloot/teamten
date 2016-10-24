package com.teamten.font;

/**
 * Describes the various variants of a typeface (regular, bold, etc.).
 */
public enum FontVariant {
    REGULAR, BOLD, ITALIC, BOLD_ITALIC, SMALL_CAPS;

    /**
     * Parses a font variant by converting it to upper case and transforming spaces and hyphens to
     * underscores, then looking it up in this enum.
     *
     * @throws IllegalArgumentException if the font variant is not found.
     */
    public static FontVariant parse(String s) {
        return valueOf(s.toUpperCase().replace(' ', '_').replace('-', '_'));
    }
}

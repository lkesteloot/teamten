package com.teamten.typeset;

/**
 * Tracks sets of fonts for a given typeface.
 */
public enum Typeface {
    TIMES_NEW_ROMAN(
            FontManager.FontName.TIMES_NEW_ROMAN,
            FontManager.FontName.TIMES_NEW_ROMAN_BOLD,
            FontManager.FontName.TIMES_NEW_ROMAN_ITALIC,
            FontManager.FontName.TIMES_NEW_ROMAN_BOLD_ITALIC,
            null),
    MINION(
            FontManager.FontName.MINION,
            FontManager.FontName.MINION_BOLD,
            FontManager.FontName.MINION_ITALIC,
            FontManager.FontName.MINION_BOLD_ITALIC,
            FontManager.FontName.MINION_SMALL_CAPS);

    private final FontManager.FontName mRegular;
    private final FontManager.FontName mBold;
    private final FontManager.FontName mItalic;
    private final FontManager.FontName mBoldItalic;
    private final FontManager.FontName mSmallCaps;

    Typeface(FontManager.FontName regular, FontManager.FontName bold, FontManager.FontName italic,
             FontManager.FontName boldItalic, FontManager.FontName smallCaps) {

        mRegular = regular;
        mBold = bold;
        mItalic = italic;
        mBoldItalic = boldItalic;
        mSmallCaps = smallCaps;
    }

    public FontManager.FontName regular() {
        return mRegular;
    }

    public FontManager.FontName bold() {
        return mBold;
    }

    public FontManager.FontName italic() {
        return mItalic;
    }

    public FontManager.FontName boldItalic() {
        return mBoldItalic;
    }

    public FontManager.FontName smallCaps() {
        return mSmallCaps;
    }
}

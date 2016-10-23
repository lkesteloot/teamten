package com.teamten.typeset;

/**
 * Tracks sets of fonts for a given typeface.
 */
public enum Typeface {
    TIMES_NEW_ROMAN(
            FontName.TIMES_NEW_ROMAN,
            FontName.TIMES_NEW_ROMAN_BOLD,
            FontName.TIMES_NEW_ROMAN_ITALIC,
            FontName.TIMES_NEW_ROMAN_BOLD_ITALIC,
            null),
    MINION(
            FontName.MINION,
            FontName.MINION_BOLD,
            FontName.MINION_ITALIC,
            FontName.MINION_BOLD_ITALIC,
            FontName.MINION_SMALL_CAPS);

    private final FontName mRegular;
    private final FontName mBold;
    private final FontName mItalic;
    private final FontName mBoldItalic;
    private final FontName mSmallCaps;

    Typeface(FontName regular, FontName bold, FontName italic,
             FontName boldItalic, FontName smallCaps) {

        mRegular = regular;
        mBold = bold;
        mItalic = italic;
        mBoldItalic = boldItalic;
        mSmallCaps = smallCaps;
    }

    public FontName regular() {
        return mRegular;
    }

    public FontName bold() {
        return mBold;
    }

    public FontName italic() {
        return mItalic;
    }

    public FontName boldItalic() {
        return mBoldItalic;
    }

    public FontName smallCaps() {
        return mSmallCaps;
    }
}

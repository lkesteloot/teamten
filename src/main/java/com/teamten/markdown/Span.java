
package com.teamten.markdown;

/**
 * A span is a horizontal sequence of letters with a specific set of attributes, like font.
 */
public class Span {
    private final String mText;
    private final boolean mIsItalic;
    private final boolean mIsSmallCaps;

    public Span(String text, boolean isItalic, boolean isSmallCaps) {
        mText = text;
        mIsItalic = isItalic;
        mIsSmallCaps = isSmallCaps;
    }

    public String getText() {
        return mText;
    }

    public boolean isItalic() {
        return mIsItalic;
    }

    public boolean isSmallCaps() {
        return mIsSmallCaps;
    }
}

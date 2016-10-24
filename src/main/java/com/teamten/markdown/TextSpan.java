
package com.teamten.markdown;

/**
 * A text span is a horizontal sequence of letters with a specific set of attributes, like font.
 */
public class TextSpan extends Span {
    private final String mText;
    private final boolean mIsItalic;
    private final boolean mIsSmallCaps;

    public TextSpan(String text, boolean isItalic, boolean isSmallCaps) {
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

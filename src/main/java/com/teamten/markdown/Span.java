
package com.teamten.markdown;

/**
 * A span is a horizontal sequence of letters with a specific set of attributes, like font.
 */
public class Span {
    private final String mText;
    private final boolean mIsItalic;

    public Span(String text, boolean isItalic) {
        mText = text;
        mIsItalic = isItalic;
    }

    public String getText() {
        return mText;
    }

    public boolean isItalic() {
        return mIsItalic;
    }
}

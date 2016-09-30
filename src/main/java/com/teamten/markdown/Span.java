
package com.teamten.markdown;

/**
 * A span is a horizontal sequence of letters with a specific set of attributes, like font.
 */
public class Span {
    private final String mText;

    public Span(String text) {
        mText = text;
    }

    public String getText() {
        return mText;
    }
}

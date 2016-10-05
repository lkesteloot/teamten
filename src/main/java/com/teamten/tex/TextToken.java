package com.teamten.tex;

/**
 * A token for plain text.
 */
public class TextToken extends Token {
    private final String mText;

    public TextToken(String text) {
        mText = text;
    }

    public String getText() {
        return mText;
    }

    @Override
    public String toString() {
        return "TextToken: " + mText;
    }
}

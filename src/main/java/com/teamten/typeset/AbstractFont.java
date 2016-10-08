package com.teamten.typeset;

import java.io.IOException;

/**
 * Parent of real font classes, with stub methods.
 */
public abstract class AbstractFont implements Font {
    private Ligatures mLigatures;

    /**
     * An abstract font with no ligature support.
     */
    public AbstractFont() {
        this(null);
    }

    /**
     * An abstract font with ligature support.
     */
    public AbstractFont(Ligatures ligatures) {
        mLigatures = ligatures;
    }

    protected void setLigatures(Ligatures ligatures) {
        mLigatures = ligatures;
    }

    @Override
    public long getKerning(int leftChar, int rightChar, double fontSize) {
        // No kerning by default.
        return 0;
    }

    @Override
    public String transformLigatures(String text) {
        return mLigatures == null ? text : mLigatures.transform(text);
    }
}

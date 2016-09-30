
package com.teamten.typeset;

/**
 * Represents a single ligature.
 */
public class Ligature {
    private final String mMulti;
    private final String mSingleString;
    private final char mSingleChar;

    public Ligature(String multi, char singleChar) {
        mMulti = multi;
        mSingleString = String.valueOf(singleChar);
        mSingleChar = singleChar;
    }

    /**
     * The multi-character version of the ligature (e.g., "fi").
     */
    public String getMulti() {
        return mMulti;
    }

    /**
     * The single-character version of the ligature (e.g., "ﬁ"), as a string.
     */
    public String getSingleString() {
        return mSingleString;
    }

    /**
     * The single-character version of the ligature (e.g., "ﬁ"), as a character.
     */
    public char getSingleChar() {
        return mSingleChar;
    }

    /**
     * Replace all instances of the ligature in the string with the single-char version.
     */
    public String replace(String s) {
        return s.replace(getMulti(), getSingleString());
    }
}


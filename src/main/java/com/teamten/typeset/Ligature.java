
package com.teamten.typeset;

/**
 * Represents a single ligature.
 */
public class Ligature {
    private final String mMulti;
    private final String mSingleString;
    private final int mSingleChar;

    public Ligature(String multi, int singleChar) {
        mMulti = multi;
        int[] codePoints = new int[1];
        codePoints[0] = singleChar;
        mSingleString = new String(codePoints, 0, 1);
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
     * The single-character version of the ligature (e.g., "ﬁ"), as a code point.
     */
    public int getSingleChar() {
        return mSingleChar;
    }

    /**
     * Replace all instances of the ligature in the string with the single-char version.
     */
    public String replace(String s) {
        return s.replace(getMulti(), getSingleString());
    }
}


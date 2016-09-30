
package com.teamten.typeset;

import com.teamten.util.RomanNumerals;

/**
 * Represents an immutable page number, either in Roman or Arabic numerals.
 */
public class PageNumber {
    private final int mNumber;
    private final boolean mIsRomanNumeral;

    /**
     * Create a new immutable page.
     *
     * @throws IllegalArgumentException if the page number is not positive.
     */
    public PageNumber(int number, boolean isRomanNumeral) {
        mNumber = number;
        mIsRomanNumeral = isRomanNumeral;

        if (mNumber == 0) {
            throw new IllegalArgumentException("cannot have a page number of zero");
        }
        if (mNumber < 0) {
            throw new IllegalArgumentException("cannot have a negative page number");
        }
    }

    /**
     * Returns the string version of the page, either in Roman or Arabic numerals.
     */
    @Override
    public String toString() {
        return mIsRomanNumeral ? RomanNumerals.toString(mNumber) : String.valueOf(mNumber);
    }

    /**
     * Return the subsequent page.
     */
    public PageNumber getNextPage() {
        return new PageNumber(mNumber + 1, mIsRomanNumeral);
    }
}


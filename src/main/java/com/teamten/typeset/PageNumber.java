
package com.teamten.typeset;

import com.teamten.util.RomanNumerals;

/**
 * Represents an immutable page number, either in Roman or Arabic numerals.
 */
public class PageNumber implements Comparable<PageNumber> {
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

    @Override
    public int compareTo(PageNumber o) {
        // Roman numerals first.
        if (mIsRomanNumeral != o.mIsRomanNumeral) {
            return mIsRomanNumeral ? -1 : 1;
        }

        // Then by page number.
        return Integer.compare(mNumber, o.mNumber);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PageNumber that = (PageNumber) o;

        if (mNumber != that.mNumber) return false;
        return mIsRomanNumeral == that.mIsRomanNumeral;

    }

    @Override
    public int hashCode() {
        int result = mNumber;
        result = 31 * result + (mIsRomanNumeral ? 1 : 0);
        return result;
    }

    /**
     * Return the subsequent page.
     */
    public PageNumber successor() {
        return new PageNumber(mNumber + 1, mIsRomanNumeral);
    }
}


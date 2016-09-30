
package com.teamten.typeset;

import java.util.TreeMap;

/**
 * Represents an immutable page number, either in Roman or Arabic numerals.
 */
public class Page {
    private final static TreeMap<Integer,String> ROMAN_MAP = new TreeMap<>();
    private final int mNumber;
    private final boolean mIsRomanNumeral;

    static {
        // Fill the map with non-additive values. Include the subtractive cases here.
        ROMAN_MAP.put(1000, "m");
        ROMAN_MAP.put(900, "cm");
        ROMAN_MAP.put(500, "d");
        ROMAN_MAP.put(400, "cd");
        ROMAN_MAP.put(100, "c");
        ROMAN_MAP.put(90, "xc");
        ROMAN_MAP.put(50, "l");
        ROMAN_MAP.put(40, "xl");
        ROMAN_MAP.put(10, "x");
        ROMAN_MAP.put(9, "ix");
        ROMAN_MAP.put(5, "v");
        ROMAN_MAP.put(4, "iv");
        ROMAN_MAP.put(1, "i");
    }

    public Page(int number, boolean isRomanNumeral) {
        mNumber = number;
        mIsRomanNumeral = isRomanNumeral;
    }

    @Override
    public String toString() {
        return mIsRomanNumeral ? toRomanNumerals(mNumber) : String.valueOf(mNumber);
    }

    /**
     * Return the subsequent page.
     */
    public Page getNextPage() {
        return new Page(mNumber + 1, mIsRomanNumeral);
    }

    static String toRomanNumerals(int number) {
        if (number == 0) {
            throw new IllegalArgumentException("roman numerals cannot represent zero");
        }

        // Not really legit, but just handle it.
        if (number < 0) {
            return "-" + toRomanNumerals(-number);
        }

        // Find greatest key below our number.
        int key = ROMAN_MAP.floorKey(number);
        if (key == number) {
            // Found exact match.
            return ROMAN_MAP.get(number);
        }

        // Use what we found and add whatever's left.
        return ROMAN_MAP.get(key) + toRomanNumerals(number - key);
    }
}


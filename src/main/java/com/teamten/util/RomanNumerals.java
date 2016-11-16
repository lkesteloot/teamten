
/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.util;

import java.util.TreeMap;

/**
 * Can convert numbers to Roman numerals.
 */
public class RomanNumerals {
    private final static TreeMap<Integer,String> ROMAN_MAP = new TreeMap<>();

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

    /**
     * Converts a number to a Roman numeral in lower case.
     *
     * @throws IllegalArgumentException if the parameter is zero.
     */
    public static String toString(int number) {
        if (number == 0) {
            throw new IllegalArgumentException("zero cannot be represented in roman numerals");
        }

        StringBuilder sb = new StringBuilder();

        // Not really legit, but just handle it.
        if (number < 0) {
            sb.append('-');
            number = -number;
        }

        while (number != 0) {
            // Find greatest key below or equal to our number.
            int key = ROMAN_MAP.floorKey(number);

            // Append what we found.
            sb.append(ROMAN_MAP.get(key));

            // Subtract what we added.
            number -= key;
        }

        return sb.toString();
    }
}

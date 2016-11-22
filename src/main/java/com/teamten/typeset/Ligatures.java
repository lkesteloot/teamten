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

package com.teamten.typeset;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Manages all ligature information for a particular font.
 */
public class Ligatures {
    private static final List<Ligature> FULL_LIST = new ArrayList<>();
    private final List<Ligature> mOrderedList;

    static {
        // All the ligatures we want to handle. There are many more, but some don't apply
        // in all cases.
        // https://en.wikipedia.org/wiki/Typographic_ligature
        FULL_LIST.add(new Ligature("ff", '\uFB00'));
        FULL_LIST.add(new Ligature("fi", '\uFB01'));
        FULL_LIST.add(new Ligature("fl", '\uFB02'));
        FULL_LIST.add(new Ligature("ffi", '\uFB03'));
        FULL_LIST.add(new Ligature("ffl", '\uFB04'));

        // Not sure I like these. The source document could just have the Unicode values,
        // though that wouldn't be the right width in a fixed-width editor like vi.
        FULL_LIST.add(new Ligature("--", '\u2013'));
        FULL_LIST.add(new Ligature("---", '\u2014'));
    }

    /**
     * A test set of ligatures that includes all the ones we know of.
     */
    public Ligatures() {
        this(x -> true);
    }

    /**
     * Create a set of ligatures for a particular font.
     *
     * @param isCharInFont return true if the Unicode character (e.g., \uFB01) is in
     * the font.
     */
    public Ligatures(Predicate<Integer> isCharInFont) {
        mOrderedList = FULL_LIST.stream()
                // If it's in the font, add it to our map.
                .filter(ligature -> isCharInFont.test(ligature.getSingleChar()))
                // Sort the ordered list by decreasing size of multi, so that "ffi" is
                // done before "fi".
                .sorted((a, b) -> -Integer.compare(a.getMulti().length(), b.getMulti().length()))
                .collect(Collectors.toList());
    }

    /**
     * Converts all ligatures to their Unicode characters.
     */
    public String transform(String s) {
        for (Ligature ligature : mOrderedList) {
            s = ligature.replace(s);
        }

        return s;
    }
}


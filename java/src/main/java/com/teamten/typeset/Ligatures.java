
package com.teamten.typeset;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Manages all ligature information.
 */
public class Ligatures {
    private static final BiMap<String,Character> MULTI_TO_SINGLE_CHAR = HashBiMap.create();
    private final BiMap<String,Character> mMultiToSingleChar = HashBiMap.create();
    private final BiMap<String,String> mMultiToSingleString = HashBiMap.create();
    private final List<Map.Entry<String,String>> mOrderedList = new ArrayList<>();

    static {
        // All the ligatures we want to handle. There are many more, but some don't apply
        // in all cases.
        // https://en.wikipedia.org/wiki/Typographic_ligature
        MULTI_TO_SINGLE_CHAR.put("ff", '\uFB00');
        MULTI_TO_SINGLE_CHAR.put("fi", '\uFB01');
        MULTI_TO_SINGLE_CHAR.put("fl", '\uFB02');
        MULTI_TO_SINGLE_CHAR.put("ffi", '\uFB03');
        MULTI_TO_SINGLE_CHAR.put("ffl", '\uFB04');

        // Not sure I like these. The source document could just have the Unicode values.
        MULTI_TO_SINGLE_CHAR.put("--", '\u2013');
        MULTI_TO_SINGLE_CHAR.put("---", '\u2014');
    }

    /**
     * Create a set of ligatures for a particular font.
     *
     * @param isCharInFont return true if the Unicode character (e.g., \uFB01) is in
     * the font.
     */
    public Ligatures(Predicate<Character> isCharInFont) {
        for (Map.Entry<String,Character> entry : MULTI_TO_SINGLE_CHAR.entrySet()) {
            String multi = entry.getKey();
            Character single = entry.getValue();

            // If it's in the font, add it to our map.
            if (isCharInFont.test(single)) {
                mMultiToSingleChar.put(multi, single);

                // Convert to the string map.
                String singleString = single.toString();
                mMultiToSingleString.put(multi, singleString);

                // Add to the ordered list.
                mOrderedList.add(new SimpleImmutableEntry<String,String>(multi, singleString));
            }
        }

        // Sort the ordered list by decreasing size of multi, so that "ffi" is done before "fi".
        Collections.sort(mOrderedList, (a, b) ->
                -Integer.compare(a.getKey().length(), b.getKey().length()));
    }

    /**
     * Converts all ligatures to their Unicode characters.
     */
    public String transform(String s) {
        for (Map.Entry<String,String> entry : mOrderedList) {
            String multi = entry.getKey();
            String single = entry.getValue();

            s = s.replace(multi, single);
        }

        return s;
    }
}


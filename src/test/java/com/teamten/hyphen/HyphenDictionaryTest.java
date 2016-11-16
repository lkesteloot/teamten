
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

package com.teamten.hyphen;

import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Test for the hyphenation class.
 */
public class HyphenDictionaryTest {
    @Test
    public void testRemoveDigits() {
        assertEquals("removeDigits", "", HyphenDictionary.removeDigits(""));
        assertEquals("removeDigits", "abc", HyphenDictionary.removeDigits("abc"));
        assertEquals("removeDigits", "abc", HyphenDictionary.removeDigits("a2b3c"));
        assertEquals("removeDigits", "'aesch", HyphenDictionary.removeDigits("'ae3s4c2h"));
        assertEquals("removeDigits", "'aêsch", HyphenDictionary.removeDigits("'aê3s4c2h"));
    }

    @Test
    public void testRemoveNonDigits() {
        assertEquals("removeNonDigits", "0", HyphenDictionary.removeNonDigits(""));
        assertEquals("removeNonDigits", "0000", HyphenDictionary.removeNonDigits("abc"));
        assertEquals("removeNonDigits", "0230", HyphenDictionary.removeNonDigits("a2b3c"));
        assertEquals("removeNonDigits", "0003420", HyphenDictionary.removeNonDigits("'ae3s4c2h"));
        assertEquals("removeNonDigits", "0003420", HyphenDictionary.removeNonDigits("'aê3s4c2h"));
        assertEquals("removeNonDigits", "0230", HyphenDictionary.removeNonDigits(".a2b3c"));
        assertEquals("removeNonDigits", "0230", HyphenDictionary.removeNonDigits("a2b3c."));
        assertEquals("removeNonDigits", "0230", HyphenDictionary.removeNonDigits(".a2b3c."));
    }

    @Test
    public void testHyphenation() {
        HyphenDictionary d = new HyphenDictionary();

        // No hyphenation at first.
        checkHyphenation(d, "successivement", "successivement");

        // Matches file.
        d.setLeftHyphenMin(2);
        d.setRightHyphenMin(2);

        // Add necessary patterns. Get these by turning on mFragmentMapDebug.
        d.addPattern(".s4");
        d.addPattern("1su");
        d.addPattern("1ce");
        d.addPattern("1si");
        d.addPattern("1ve");
        d.addPattern("1me");
        d.addPattern(".1su");

        // Now should work.
        checkHyphenation(d, "successivement", "suc", "ces", "si", "ve", "ment");

        // Try mins.
        d.setLeftHyphenMin(3);
        d.setRightHyphenMin(4);
        checkHyphenation(d, "successivement", "suc", "ces", "si", "ve", "ment");

        // Try mins.
        d.setLeftHyphenMin(4);
        d.setRightHyphenMin(5);
        checkHyphenation(d, "successivement", "succes", "si", "vement");
    }

    @Test
    public void testFrenchDictionary() throws IOException {
        HyphenDictionary d = HyphenDictionary.fromResource("fr");

        // This was once a problem where the hyphenator returned "su", "per", "-", "confort". The hyphen
        // should be with the "per".
        checkHyphenation(d, "super-confort", "su", "per-", "confort");
    }

    @Test
    public void testEnglishDictionary() throws IOException {
        HyphenDictionary d = HyphenDictionary.fromResource("en_US");

        // This was once a problem where the hyphenator returned "back", "-end" for "back-end". The hyphen
        // should be with the "back-".
        checkHyphenation(d, "back-end", "back-", "end");
    }

    private void checkHyphenation(HyphenDictionary d, String word, String ... expectedFragments) {
        List<String> actualFragments = d.hyphenate(word);

        if (false) {
            for (String s : expectedFragments) {
                System.out.printf("<%s>  ", s);
            }
            System.out.println();
            for (String s : actualFragments) {
                System.out.printf("<%s>  ", s);
            }
            System.out.println();
        }

        assertArrayEquals("word " + word + " failed", expectedFragments,
                actualFragments.toArray());
    }
}


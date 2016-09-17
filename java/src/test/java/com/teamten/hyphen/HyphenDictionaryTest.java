
package com.teamten.hyphen;

import org.junit.Test;

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

        d.addPattern(".s4");
        d.addPattern("1su");
        d.addPattern("1ce");
        d.addPattern("1si");
        d.addPattern("1ve");
        d.addPattern("1me");
        d.addPattern(".1su");

        // Now should work.
        checkHyphenation(d, "successivement", "suc", "ces", "si", "ve", "ment");
    }

    private void checkHyphenation(HyphenDictionary d, String word, String ... expectedFragments) {
        List<String> actualFragments = d.hyphenate(word);

        assertArrayEquals("word " + word + " failed", expectedFragments,
                actualFragments.toArray());
    }
}


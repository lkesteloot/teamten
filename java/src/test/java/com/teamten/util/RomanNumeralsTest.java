
package com.teamten.typeset;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the Page class.
 */
public class PageTest {
    @Test
    public void testRomanNumerals() {
        assertEquals("i", Page.toRomanNumerals(1));
        assertEquals("ii", Page.toRomanNumerals(2));
        assertEquals("iii", Page.toRomanNumerals(3));
        assertEquals("iv", Page.toRomanNumerals(4));
        assertEquals("v", Page.toRomanNumerals(5));
        assertEquals("vi", Page.toRomanNumerals(6));
        assertEquals("vii", Page.toRomanNumerals(7));
        assertEquals("viii", Page.toRomanNumerals(8));
        assertEquals("ix", Page.toRomanNumerals(9));
        assertEquals("x", Page.toRomanNumerals(10));
        assertEquals("-xi", Page.toRomanNumerals(-11));
        assertEquals("mmmmmmmmmmmm", Page.toRomanNumerals(12000));
    }
}


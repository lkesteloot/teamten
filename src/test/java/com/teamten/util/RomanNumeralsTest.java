
package com.teamten.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test the RomanNumerals class.
 */
public class RomanNumeralsTest {
    @Test
    public void testRomanNumerals() {
        assertEquals("i", RomanNumerals.toString(1));
        assertEquals("ii", RomanNumerals.toString(2));
        assertEquals("iii", RomanNumerals.toString(3));
        assertEquals("iv", RomanNumerals.toString(4));
        assertEquals("v", RomanNumerals.toString(5));
        assertEquals("vi", RomanNumerals.toString(6));
        assertEquals("vii", RomanNumerals.toString(7));
        assertEquals("viii", RomanNumerals.toString(8));
        assertEquals("ix", RomanNumerals.toString(9));
        assertEquals("x", RomanNumerals.toString(10));
        assertEquals("-xi", RomanNumerals.toString(-11));
        assertEquals("mmmmmmmmmmmm", RomanNumerals.toString(12000));
    }
}


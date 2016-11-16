
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


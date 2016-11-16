
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

import org.junit.Test;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import static org.junit.Assert.*;

/**
 * Test for the SpaceUnit class.
 */
public class SpaceUnitTest {
    @Test
    public void testConversionToSp() {
        assertEquals(65536L, SpaceUnit.PT.toSp(1.0));
        assertEquals(25*65536L/10, SpaceUnit.PT.toSp(2.5));
    }

    @Test
    public void testConversionFromSp() {
        assertEquals(1, SpaceUnit.PT.fromSp(65536), 0.001);
    }

    @Test
    public void testParsing() throws IOException {
        assertEquals(65536L, parseString("1 pt*"));
        assertEquals(65536L, parseString("1pt*"));
        assertEquals(12*65536L, parseString("1pc*"));
        assertEquals(72*65536L, parseString("1in*"));
        assertEquals(72*65536L, parseString("1.0in*"));
        assertEquals(72*65536L, parseString("1.000in*"));
        assertEquals(-72*65536L + 1, parseString("-1.000in*"));
        assertEquals(-72*65536L + 1, parseString("-10.0e-1 in*"));
        assertEquals(1857713L, parseString("1 cm*"));
        assertEquals(185771L, parseString("1 mm*"));
    }

    @Test
    public void testFailedParsing() throws IOException {
        // All of these should fail.
        for (String s : new String[] {
            "",
            "abc",
            "56",
            "5-5 pt",
            "55 em"
        }) {

            try {
                parseString(s);
                fail("should have failed to parse " + s);
            } catch (NumberFormatException e) {
                // No problem.
            }
        }
    }

    private long parseString(String s) throws IOException {
        Reader r = new StringReader(s);
        long distance = SpaceUnit.parseDistance(r);

        // Verify that we stopped at the correct character.
        assertEquals('*', r.read());

        r.close();

        return distance;
    }
}


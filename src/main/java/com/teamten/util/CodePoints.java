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

import java.io.IOException;
import java.io.Reader;

/**
 * Utilities for dealing with code points.
 */
public class CodePoints {

    /**
     * Return the next code point in the reader, or -1 on end of file.
     */
    public static int nextCodePoint(Reader reader) throws IOException {
        int ch = reader.read();
        if (ch != -1 && Character.isHighSurrogate((char) ch)) {
            int high = ch;
            int low = reader.read();
            if (low == -1) {
                ch = -1;
            } else {
                ch = Character.toCodePoint((char) high, (char) low);
            }
        }

        return ch;
    }

    /**
     * Simple routine to convert a single code point to a string, because surprisingly this isn't a thing
     * in the standard library.
     */
    public static String toString(int codePoint) {
        int[] codePoints = new int[1];
        codePoints[0] = codePoint;
        return new String(codePoints, 0, 1);
    }
}

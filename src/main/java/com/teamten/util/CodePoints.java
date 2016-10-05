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

package com.teamten.tex;

import java.util.HashMap;
import java.util.Map;

/**
 * The commands we know about, and how to encode them as code points.
 */
public class Command {
    private static final String[] KEYWORDS = {
        "hbox",
    };
    // https://en.wikipedia.org/wiki/Private_Use_Areas
    private static final int PRIVATE_USE_AREA_1 = 0xE000;
    private static final int PRIVATE_USE_AREA_2_START = 0xF0000;
    private static final int PRIVATE_USE_AREA_2_LAST = 0xFFFFD;

    /**
     * Return the pseudo-codepoint for the keyword, or -1 if not found.
     */
    public static int fromKeyword(String keyword) {
        for (int i = 0; i < KEYWORDS.length; i++) {
            if (keyword.equals(KEYWORDS[i])) {
                return PRIVATE_USE_AREA_1 + i;
            }
        }

        return -1;
    }

    /**
     * Return the keyword for the character, or null if it doesn't map to a keyword.
     */
    public static String toKeyword(int ch) {
        int i = ch - PRIVATE_USE_AREA_1;
        if (i >= 0 && i < KEYWORDS.length) {
            return KEYWORDS[i];
        } else {
            return null;
        }
    }

    /**
     * Return the pseudo-codepoint for the character command.
     * @throws IllegalArgumentException if the character is too large to fit in the private use area.
     */
    public static int fromCharacter(int ch) {
        int token = ch + PRIVATE_USE_AREA_2_START;
        if (token < PRIVATE_USE_AREA_2_START || token > PRIVATE_USE_AREA_2_LAST) {
            throw new IllegalArgumentException("character " + ch + " not legal character token");
        }

        return token;
    }
}

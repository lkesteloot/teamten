package com.teamten.tex;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.teamten.util.CodePoints;

/**
 * The commands we know about, and how to encode them as code points.
 */
public class Token {
    // https://en.wikipedia.org/wiki/Private_Use_Areas
    private static final int PRIVATE_USE_AREA_1 = 0xE000;
    private static final int PRIVATE_USE_AREA_2_START = 0xF0000;
    private static final int PRIVATE_USE_AREA_2_LAST = 0xFFFFD;
    public static final int HBOX = PRIVATE_USE_AREA_1;
    public static final int GLUE = PRIVATE_USE_AREA_1 + 1;
    public static final int PLUS = PRIVATE_USE_AREA_1 + 2;
    public static final int MINUS = PRIVATE_USE_AREA_1 + 3;
    public static final int PENALTY = PRIVATE_USE_AREA_1 + 4;
    private static final BiMap<Integer,String> TOKEN_TO_KEYWORD = HashBiMap.create();
    private static final BiMap<String,Integer> KEYWORD_TO_TOKEN = TOKEN_TO_KEYWORD.inverse();

    static {
        TOKEN_TO_KEYWORD.put(HBOX, "hbox");
        TOKEN_TO_KEYWORD.put(GLUE, "glue");
        TOKEN_TO_KEYWORD.put(PLUS, "plus");
        TOKEN_TO_KEYWORD.put(MINUS, "minus");
        TOKEN_TO_KEYWORD.put(PENALTY, "penalty");
    }

    /**
     * Return the pseudo-codepoint for the keyword, or -1 if not found.
     */
    public static int fromKeyword(String keyword) {
        return KEYWORD_TO_TOKEN.getOrDefault(keyword, -1);
    }

    /**
     * Return the keyword for the character, or null if it doesn't map to a keyword.
     */
    public static String toKeyword(int ch) {
        return TOKEN_TO_KEYWORD.get(ch);
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

    /**
     * Return the code point for the original command, or -1 if not a character command.
     */
    public static int toCharacter(int ch) {
        if (ch >= PRIVATE_USE_AREA_2_START && ch <= PRIVATE_USE_AREA_2_LAST) {
            return ch - PRIVATE_USE_AREA_2_START;
        } else {
            return -1;
        }
    }

    /**
     * Convert to a user-friendly command, with backslash prefix if necessary.
     */
    public static String toString(int token) {
        String keyword = toKeyword(token);
        if (keyword != null) {
            return "\\" + keyword;
        }

        int ch = toCharacter(token);
        if (ch != -1) {
            return new StringBuilder().append('\\').appendCodePoint(ch).toString();
        }

        switch (token) {
            case ' ':
                return "(space)";

            case '\n':
                return "(return)";

            default:
                return CodePoints.toString(token);
        }
    }
}

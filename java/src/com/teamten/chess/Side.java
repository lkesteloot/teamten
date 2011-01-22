// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

/**
 * Sides are represented as an integer, with 0 for white and 1 for black. This
 * class provides utility methods and constants.
 */
public class Side {
    public static final int WHITE = 0;
    public static final int BLACK = 1;

    private Side() {
        // Can't instantiate.
    }

    /**
     * Converts the piece character (such as "b" for bishop) to
     * upper or lower case as appropriate for the side.
     */
    public static char convertCharacter(int side, char ch) {
        if (side == WHITE) {
            return Character.toUpperCase(ch);
        } else {
            return Character.toLowerCase(ch);
        }
    }

    /**
     * Return the offset from 0x2654 for this color.
     *
     * See http://en.wikipedia.org/wiki/Chess_symbols_in_Unicode
     */
    public static int getUnicodeOffset(int side) {
        return side == WHITE ? 0 : 6;
    }

    /**
     * Given a side, returns the other side.
     */
    public static int getOtherSide(int side) {
        return 1 - side;
    }

    /**
     * Returns a string version of the side ("White" or "Black").
     */
    public static String toString(int side) {
        if (side == WHITE) {
            return "White";
        } else {
            return "Black";
        }
    }
}

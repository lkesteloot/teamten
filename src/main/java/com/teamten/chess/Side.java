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

// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

/**
 * Sides are represented as an integer, with 0 for white and 1 for black. This
 * class provides utility methods and constants.
 */
public class Side {
    public static final int WHITE = 0;
    public static final int BLACK = 1;
    // Pseudo-sides for game results. Not all functions of this class support these.
    public static final int DRAW = 2;
    public static final int IN_PROGRESS = 3;

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
        switch (side) {
            case WHITE:
                return "White";

            case BLACK:
                return "Black";

            case DRAW:
                return "Draw";

            case IN_PROGRESS:
                return "In progress";
        }

        return "?";
    }

    /**
     * Get the PGN notation for which side won.
     */
    public static String toPgnNotation(int side) {
        switch (side) {
            case WHITE:
                return "1-0";

            case BLACK:
                return "0-1";

            case DRAW:
                return "1/2-1/2";

            case IN_PROGRESS:
                return "*";
        }

        return "?";
    }
}

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

package com.teamten.tictactoe;

/**
 * Represents a mark on the board.
 */
public class Mark {
    public static final int EMPTY = 0;
    public static final int CROSS = 1;
    public static final int NOUGHT = 2;

    /**
     * Return a character for the mark ('X', 'O', or space).
     */
    public static char getCharacter(int mark) {
        switch (mark) {
            case EMPTY:
                return ' ';

            case CROSS:
                return 'X';

            case NOUGHT:
                return 'O';

            default:
                return '?';
        }
    }

    /**
     * Return an English word for the side (Cross, etc.).
     */
    public static String toString(int mark) {
        switch (mark) {
            case CROSS:
                return "Cross";

            case NOUGHT:
                return "Naught";

            default:
                return "?";
        }
    }

    /**
     * Given a mark, return the other side (cross for nought, etc.).
     */
    public static int getOtherSide(int mark) {
        switch (mark) {
            case CROSS:
                return NOUGHT;

            case NOUGHT:
                return CROSS;

            default:
                throw new IllegalArgumentException();
        }
    }
}

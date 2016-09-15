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

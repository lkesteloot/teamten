// Copyright 2011 Lawrence Kesteloot

package com.teamten.tictactoe;

/**
 * Represents a board of a Tic-Tac-Toe game.
 */
public class Board {
    public static final int SIZE = 3;
    public static final int NUM_SQUARES = SIZE*SIZE;
    // Row-major order, from top-left to bottom-right.
    private final int[] mMarks = new int[NUM_SQUARES];
    private int mNumMarks = 0;

    /**
     * Sets the mark (see Mark class) at index.
     */
    public void setMark(int index, int mark) {
        mMarks[index] = mark;
    }

    /**
     * Returns the mark (see Mark class) at index.
     */
    public int getMark(int index) {
        return mMarks[index];
    }

    /**
     * Return whose side (see Mark class) it is to play.
     */
    public int getSide() {
        return (mNumMarks % 2 == 0) ? Mark.CROSS : Mark.NOUGHT;
    }

    /**
     * Has the current side make a mark at this move.
     */
    public void makeMove(int index) {
        setMark(index, getSide());
        mNumMarks++;
    }

    /**
     * Undoes a move at index.
     */
    public void undoMove(int index) {
        setMark(index, Mark.EMPTY);
        mNumMarks--;
    }

    /**
     * Whether the board is full of marks.
     */
    public boolean isFull() {
        return mNumMarks == NUM_SQUARES;
    }

    /**
     * File and rank are as in chess (1-based).
     */
    public static int getIndex(int file, int rank) {
        return (file - 1) + (SIZE - rank)*SIZE;
    }

    /**
     * Returns a side that has won, or EMPTY if neither.
     */
    public int getWinner() {
        // This is hard-coded for SIZE == 3.

        // Wins that include center.
        if (mMarks[4] != Mark.EMPTY
                && ((mMarks[4] == mMarks[0] && mMarks[4] == mMarks[8])
                    || (mMarks[4] == mMarks[1] && mMarks[4] == mMarks[7])
                    || (mMarks[4] == mMarks[2] && mMarks[4] == mMarks[6])
                    || (mMarks[4] == mMarks[3] && mMarks[4] == mMarks[5]))) {

            return mMarks[4];
        }

        // Wins that include upper-left.
        if (mMarks[0] != Mark.EMPTY
                && ((mMarks[0] == mMarks[1] && mMarks[0] == mMarks[2])
                    || (mMarks[0] == mMarks[3] && mMarks[0] == mMarks[6]))) {

            return mMarks[0];
        }

        // Wins that include upper-right.
        if (mMarks[2] != Mark.EMPTY && mMarks[2] == mMarks[5] && mMarks[2] == mMarks[8]) {
            return mMarks[2];
        }

        // Wins that include bottom left.
        if (mMarks[6] != Mark.EMPTY && mMarks[6] == mMarks[7] && mMarks[6] == mMarks[8]) {
            return mMarks[6];
        }

        return Mark.EMPTY;
    }

    /**
     * Prints the board to the console with the specified indent.
     */
    public void print(String indent) {
        for (int rank = SIZE; rank >= 1; rank--) {
            System.out.print(indent);
            for (int file = 1; file <= SIZE; file++) {
                int mark = getMark(getIndex(file, rank));
                char ch = Mark.getCharacter(mark);

                System.out.print(' ');
                System.out.print(ch);
                System.out.print(' ');
                if (file < SIZE) {
                    System.out.print('|');
                }
            }
            System.out.println();
            if (rank > 1) {
                for (int file = 1; file <= SIZE; file++) {
                    System.out.print("---");
                    if (file < SIZE) {
                        System.out.print('+');
                    }
                }
                System.out.println();
            }
        }
        int winner = getWinner();
        if (winner != Mark.EMPTY) {
            System.out.println(Mark.toString(winner) + " has won");
        } else if (isFull()) {
            System.out.println("The game is a draw");
        }
        System.out.println();
    }
}

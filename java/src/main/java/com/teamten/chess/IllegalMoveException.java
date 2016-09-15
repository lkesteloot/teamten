// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

/**
 * Thrown when a user tries to make a move that's not valid or legal.
 */
public class IllegalMoveException extends Exception {
    /**
     * Location of the piece that would be putting the king in check.
     */
    private final int mCheckIndex;

    public IllegalMoveException(String message) {
        this(message, -1);
    }

    public IllegalMoveException(String message, int checkIndex) {
        super(message);
        mCheckIndex = checkIndex;
    }

    /**
     * If this move is illegal because the king would be in check, returns
     * one of pieces that would be putting it in check. Otherwise returns -1.
     */
    public int getCheckIndex() {
        return mCheckIndex;
    }
}


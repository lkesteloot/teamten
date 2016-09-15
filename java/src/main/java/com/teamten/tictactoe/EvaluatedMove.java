// Copyright 2011 Lawrence Kesteloot

package com.teamten.tictactoe;

/**
 * Tracks a move, its score, and the principal variation from here.
 */
public class EvaluatedMove {
    private int mIndex;
    private double mScore;
    private EvaluatedMove mNextMove;

    /**
     * The index where the move was made.
     */
    public void setIndex(int index) {
        mIndex = index;
    }

    public int getIndex() {
        return mIndex;
    }

    /**
     * The score for this player after this move is made.
     */
    public void setScore(double score) {
        mScore = score;
    }

    public double getScore() {
        return mScore;
    }

    /**
     * Principal variation linked list.
     */
    public void setNextMove(EvaluatedMove nextMove) {
        mNextMove = nextMove;
    }

    public EvaluatedMove getNextMove() {
        return mNextMove;
    }

    /**
     * Dumps this move and its principal variation to the console.
     */
    public void printPrincipalVariation() {
        System.out.print("Principal variation:");
        for (EvaluatedMove move = this; move != null; move = move.getNextMove()) {
            System.out.printf(" %d (%.2f)", move.getIndex(), move.getScore());
        }
        System.out.println();
    }
}

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

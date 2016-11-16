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
 * Plays the computer side of the game.
 */
public class ComputerPlayer {
    private static final int WIN_SCORE = 100;
    private final boolean mSmart;
    private final int mMaxDepth;

    public ComputerPlayer() {
        this(false, 100);
    }

    public ComputerPlayer(boolean smart, int maxDepth) {
        mSmart = smart;
        mMaxDepth = maxDepth;
    }

    /**
     * Get a move to make with its score.
     */
    public EvaluatedMove getMove(Board board) {
        return getMove(board, 0);
    }

    /**
     * Recursive method with depth.
     */
    private EvaluatedMove getMove(Board board, int depth) {
        int side = board.getSide();
        EvaluatedMove bestMove = new EvaluatedMove();

        bestMove.setIndex(-1);
        bestMove.setScore(-Double.MAX_VALUE);
        bestMove.setNextMove(null);

        // See if we're at a terminal node.
        int winner = board.getWinner();
        if (winner != Mark.EMPTY) {
            double score = WIN_SCORE + Math.random() - 0.5 - depth;
            if (winner == side) {
                bestMove.setScore(score);
            } else {
                bestMove.setScore(-score);
            }
            return bestMove;
        }
        if (board.isFull() || depth >= mMaxDepth) {
            bestMove.setScore(0);
            return bestMove;
        }

        // Explore tree.
        double bestRandomScore = -Double.MAX_VALUE;
        int numLosses = 0;
        for (int index = 0; index < Board.NUM_SQUARES; index++) {
            if (board.getMark(index) == Mark.EMPTY) {
                // Try it.
                board.makeMove(index);

                EvaluatedMove subMove = getMove(board, depth + 1);
                double subScore = -subMove.getScore();
                if (subScore < -WIN_SCORE/2) {
                    numLosses++;
                }
                double subRandomScore = subScore + Math.random() - 0.5;
                if (subRandomScore > bestRandomScore) {
                    bestMove.setIndex(index);
                    bestMove.setScore(subScore);
                    bestRandomScore = subRandomScore;
                    if (subMove.getIndex() != -1) {
                        bestMove.setNextMove(subMove);
                    } else {
                        bestMove.setNextMove(null);
                    }
                }

                board.undoMove(index);
            }
        }

        double finalScore = bestMove.getScore();
        if (mSmart) {
            finalScore -= numLosses;
        }
        bestMove.setScore(finalScore);

        return bestMove;
    }
}

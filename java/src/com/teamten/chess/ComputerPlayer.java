// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Plays chess.
 */
public class ComputerPlayer {
    private static final int MAX_DEPTH = 4;
    private static final int MAX_QUIESCENCE_DEPTH = MAX_DEPTH*2;
    private final Board mBoard;
    private final Game mGame;
    private final int mSide;
    private long mTimeOfLastPrint;
    private int mMovesConsidered;

    /**
     * Create a player for a side in a game.
     */
    public ComputerPlayer(Board board, Game game, int side) {
        mBoard = board;
        mGame = game;
        mSide = side;
    }

    /**
     * Makes a move on this board. Returns the move made along with a linked list of
     * the principal variation.
     */
    public EvaluatedMove makeMove() {
        long beforeTime = System.currentTimeMillis();
        mTimeOfLastPrint = beforeTime;
        mMovesConsidered = 0;
        EvaluatedMove evaluatedMove = getBestMove(0.0, 0, mSide, new ArrayList<Move>(),
                -10000, 10000, false, false, 1);
        if (evaluatedMove.mMove == null) {
            System.out.println(Side.toString(mSide) + " cannot move, end of game");
        } else {
            long afterTime = System.currentTimeMillis();
            Move move = evaluatedMove.mMove;
            System.out.printf("%s makes move %s with score %f (%d ms, %,d moves considered)%n",
                    Side.toString(mSide), move, evaluatedMove.mScore,
                    afterTime - beforeTime, mMovesConsidered);
            System.out.print("Principal variation:");
            for (EvaluatedMove e = evaluatedMove; e != null && e.mMove != null; e = e.mNextMove) {
                System.out.print(" " + e.mMove);
            }
            System.out.println();
            mGame.addMove(move);
        }

        return evaluatedMove;
    }

    /**
     * Make a move for this particular side.
     */
    private EvaluatedMove getBestMove(double boardValue, int depth, int side, List<Move> allMoves,
            double alpha, double beta, boolean noisyMove, boolean noisyCheckMove, int color) {

        // Cap the search at a depth, unless the last move is noisy.
        // See http://en.wikipedia.org/wiki/Quiescence_search
        if (depth >= MAX_DEPTH && (!noisyMove || depth >= MAX_QUIESCENCE_DEPTH)) {
            return new EvaluatedMove(null, color*boardValue, null);
        }

        long now = System.currentTimeMillis();
        if (now - mTimeOfLastPrint >= 1000) {
            System.out.println("Considering moves " + allMoves);
            mTimeOfLastPrint = now;
        }

        // Generate all moves for this side.
        List<Move> moveList = mBoard.generateAllMoves(side, false);

        // Remove the ones that would put us in check.
        mBoard.removeIllegalMoves(moveList);

        // If we have no legal moves left, then it's either stalemate or checkmate.
        if (moveList.isEmpty()) {
            if (mBoard.getCheckIndex(side) != -1) {
                // Checkmate. Add 10 points per ply so that we checkmate as soon as possible.
                return new EvaluatedMove(null, -1000 + depth*10, null);
            } else {
                // Stalemate.
                // XXX here this should be absolute zero so that our decision depends
                // on whether we're behind.
                return new EvaluatedMove(null, 0, null);
            }
        }

        // Sort by good move.
        Collections.sort(moveList, Move.CAPTURE_COMPARATOR);

        // Pick a good move.
        Move bestMove = null;
        EvaluatedMove bestSubEvaluatedMove = null;

        for (Move move : moveList) {
            mMovesConsidered++;

            allMoves.add(move);

            double moveBoardValue = boardValue;

            // Account for piece loss.
            noisyMove = noisyCheckMove; // Response to being in check is also noisy.
            Piece piece = move.getCapturedPiece();
            if (piece != Piece.EMPTY) {
                moveBoardValue += color*piece.getPieceType().getValue();
                noisyMove = true;
            }
            piece = move.getPromotedPiece();
            if (piece != Piece.EMPTY) {
                moveBoardValue -= color*PieceType.PAWN.getValue();
                moveBoardValue += color*piece.getPieceType().getValue();
                noisyMove = true;
            }

            if (mBoard.isEndGame()) {
                // Advance pawns in endgame.
                if (move.getMovingPiece().getPieceType() == PieceType.PAWN) {
                    moveBoardValue += color*0.3;
                }
            } else {
                // Account for control of the center (by any piece).
                if (Board.isCenterSquare(move.getFromIndex())) {
                    moveBoardValue -= color*0.1;
                }
                if (Board.isCenterSquare(move.getToIndex())) {
                    moveBoardValue += color*0.1;
                }
            }

            // Add a bit of randomness to break ties.
            moveBoardValue += Math.random()*0.001;

            mGame.addMove(move);
            boolean checkMove = false;
            if (move.isCheck()) {
                noisyMove = true;
                checkMove = true;
            }
            EvaluatedMove subEvaluatedMove = getBestMove(moveBoardValue, depth + 1,
                    Side.getOtherSide(side), allMoves, -beta, -alpha, noisyMove, checkMove,
                    -color);
            // A good score for them is a bad score for us.
            double moveAlpha = -subEvaluatedMove.mScore;
            mGame.undoMove();

            /// System.out.println("Move " + move + " has score " + score);

            if (moveAlpha > alpha) {
                alpha = moveAlpha;
                bestMove = move;
                bestSubEvaluatedMove = subEvaluatedMove;
            }

            allMoves.remove(allMoves.size() - 1);

            // Alpha-beta pruning.
            if (beta <= alpha) {
                break;
            }
        }

        return new EvaluatedMove(bestMove, alpha, bestSubEvaluatedMove);
    }

    /**
     * Stores a move and its score.
     */
    public static class EvaluatedMove {
        private Move mMove;
        private double mScore;
        private EvaluatedMove mNextMove;

        public EvaluatedMove(Move move, double score, EvaluatedMove nextMove) {
            mMove = move;
            mScore = score;
            mNextMove = nextMove;
        }

        public void setMove(Move move) {
            mMove = move;
        }

        public Move getMove() {
            return mMove;
        }

        /**
         * Positive scores are good for the side making the move.
         */
        public void setScore(double score) {
            mScore = score;
        }

        public double getScore() {
            return mScore;
        }

        /**
         * Keep track of next move for principal variation.
         */
        public void setNextMove(EvaluatedMove nextMove) {
            mNextMove = nextMove;
        }

        public EvaluatedMove getNextMove() {
            return mNextMove;
        }
    }
}

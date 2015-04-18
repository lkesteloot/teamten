// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Plays chess.
 */
public class ComputerPlayer {
    private final Board mBoard;
    private final Game mGame;
    private final int mSide;
    private long mTimeOfLastPrint;
    private long mMovesConsidered;
    private long mStopTime;

    /**
     * Create a player for a side in a game.
     */
    public ComputerPlayer(Board board, Game game, int side) {
        mBoard = board;
        mGame = game;
        mSide = side;
    }

    public int getSide() {
        return mSide;
    }

    /**
     * Makes a move on this board. Returns the move made along with a linked list of
     * the principal variation.
     */
    public Result makeMove(long moveTime) {
        long beforeTime = System.currentTimeMillis();
        mTimeOfLastPrint = beforeTime;
        mMovesConsidered = 0;
        mStopTime = beforeTime + moveTime;
        EvaluatedMove bestEvaluatedMove = null;

        int maxDepth = 2;
        while (true) {
            EvaluatedMove evaluatedMove = getBestMove(0.0, 0, maxDepth, mSide,
                    new ArrayList<Move>(), -10000, 10000, false, false, 1);

            long now = System.currentTimeMillis();
            System.out.println(maxDepth + " " + (now - beforeTime));
            if (now >= mStopTime || evaluatedMove == null || evaluatedMove.getMove() == null) {
                break;
            }

            bestEvaluatedMove = evaluatedMove;

            maxDepth++;
        }

        if (bestEvaluatedMove == null) {
            // Can't really happen, it means that even at minimal depth we ran out
            // of time. I suppose we could have been passed a very short time window,
            // like when we're running out of time. We should force the minimal depth
            // search to always complete.
            bestEvaluatedMove = new EvaluatedMove(null, 0, null);
        }

        Move move = bestEvaluatedMove.getMove();
        if (move != null) {
            mGame.addMove(move);
        }
        long afterTime = System.currentTimeMillis();

        return new Result(bestEvaluatedMove, afterTime - beforeTime, mMovesConsidered);
    }

    /**
     * Make a move for this particular side.
     */
    private EvaluatedMove getBestMove(double boardValue, int depth, int maxDepth, int side,
            List<Move> allMoves, double alpha, double beta, boolean noisyMove,
            boolean noisyCheckMove, int color) {

        // Cap the search at a depth, unless the last move is noisy.
        // See http://en.wikipedia.org/wiki/Quiescence_search
        if (depth >= maxDepth && (!noisyMove || depth >= maxDepth*2)) {
            return new EvaluatedMove(null, color*boardValue, null);
        }

        if (mMovesConsidered % 10000 == 0) {
            long now = System.currentTimeMillis();
            if (now > mStopTime) {
                // Return an error, we know nothing about this node.
                return null;
            }

            if (false && now - mTimeOfLastPrint >= 1000) {
                System.out.printf("Considering moves %s (%.2f)%n", allMoves, alpha);
                mTimeOfLastPrint = now;
            }
        }

        // Generate all moves for this side.
        List<Move> moveList = mBoard.generateAllMoves(side, false);

        // Update all their check status.
        for (Move move : moveList) {
            mBoard.updateMoveCheckStatus(move);
        }

        // Can't put yourself in check.
        Iterator<Move> itr = moveList.iterator();
        while (itr.hasNext()) {
            Move move = itr.next();
            if (move.isMovingInCheck()) {
                itr.remove();
            }
        }

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
            EvaluatedMove subEvaluatedMove = getBestMove(moveBoardValue, depth + 1, maxDepth,
                    Side.getOtherSide(side), allMoves, -beta, -alpha, noisyMove, checkMove,
                    -color);
            mGame.undoMove();
            if (subEvaluatedMove == null) {
                // Out of time.
                return null;
            }
            // A good score for them is a bad score for us.
            double moveAlpha = -subEvaluatedMove.mScore;

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

    /**
     * Stores the result of the computer's move.
     */
    public class Result {
        public final EvaluatedMove mEvaluatedMove;
        public final long mElapsedTime;
        public final long mMovesConsidered;

        public Result(EvaluatedMove evaluatedMove, long elapsedTime, long movesConsidered) {
            mEvaluatedMove = evaluatedMove;
            mElapsedTime = elapsedTime;
            mMovesConsidered = movesConsidered;
        }
    }
}

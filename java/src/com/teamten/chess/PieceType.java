// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a type of piece, such as a queen or rook, but not of a particular
 * color.
 */
public abstract class PieceType {
    public static final PieceType PAWN = new PieceType('p', 5, 1) {
        private final int[] POSITION_BONUS = new int[]{
             0,  0,  0,  0,  0,  0,  0,  0,
            50, 50, 50, 50, 50, 50, 50, 50,
            10, 10, 20, 30, 30, 20, 10, 10,
             5,  5, 10, 27, 27, 10,  5,  5,
             0,  0,  0, 25, 25,  0,  0,  0,
             5, -5,-10,  0,  0,-10, -5,  5,
             5, 10, 10,-25,-25, 10, 10,  5,
             0,  0,  0,  0,  0,  0,  0,  0
        };

        @Override // PieceType
        public void addMoves(Board board, int index, int side, boolean capturesOnly,
                List<Move> moveList) {

            int rankDirection;
            Piece promotedPiece = Piece.EMPTY;
            if (side == Side.WHITE) {
                rankDirection = 1;
                if (Board.getRank(index) == 7) {
                    promotedPiece = Piece.WHITE_QUEEN;
                }
            } else {
                rankDirection = -1;
                if (Board.getRank(index) == 2) {
                    promotedPiece = Piece.BLACK_QUEEN;
                }
            }

            // Captures.
            int otherIndex = board.getRelativeIndex(index, -1, rankDirection);
            if (otherIndex != -1) {
                Piece piece = board.getPiece(otherIndex);
                if (piece.isOtherSide(side)) {
                    moveList.add(Move.makePromotion(board, index, otherIndex, promotedPiece));
                }
            }

            otherIndex = board.getRelativeIndex(index, 1, rankDirection);
            if (otherIndex != -1) {
                Piece piece = board.getPiece(otherIndex);
                if (piece.isOtherSide(side)) {
                    moveList.add(Move.makePromotion(board, index, otherIndex, promotedPiece));
                }
            }

            // Moves.
            if (!capturesOnly) {
                otherIndex = board.getRelativeIndex(index, 0, rankDirection);
                if (otherIndex != -1 && board.getPiece(otherIndex) == Piece.EMPTY) {
                    moveList.add(Move.makePromotion(board, index, otherIndex, promotedPiece));

                    if ((side == Side.WHITE && Board.getRank(index) == 2) ||
                            (side == Side.BLACK && Board.getRank(index) == 7)) {

                        otherIndex = board.getRelativeIndex(index, 0, rankDirection*2);
                        if (otherIndex != -1 && board.getPiece(otherIndex) == Piece.EMPTY) {
                            moveList.add(Move.makePromotion(board, index,
                                        otherIndex, promotedPiece));
                        }
                    }
                }
            }
        }

        @Override // Object
        public String toString() {
            return "pawn";
        }

        @Override // PieceType
        public double getPositionBonus(int index) {
            return POSITION_BONUS[index]/100.0;
        }
    };
    public static final PieceType BISHOP = new PieceType('b', 3, 3) {
        private final int[] POSITION_BONUS = new int[]{
            -20,-10,-10,-10,-10,-10,-10,-20,
            -10,  0,  0,  0,  0,  0,  0,-10,
            -10,  0,  5, 10, 10,  5,  0,-10,
            -10,  5,  5, 10, 10,  5,  5,-10,
            -10,  0, 10, 10, 10, 10,  0,-10,
            -10, 10, 10, 10, 10, 10, 10,-10,
            -10,  5,  0,  0,  0,  0,  5,-10,
            -20,-10,-40,-10,-10,-40,-10,-20,
        };

        // Pair of numbers to provide the four directions, starting up-right
        // and going clockwise.
        private final int[] FILE_DELTAS = new int[] { 1, 1, -1, -1 };
        private final int[] RANK_DELTAS = new int[] { 1, -1, -1, 1 };

        @Override // PieceType
        public void addMoves(Board board, int index, int side, boolean capturesOnly,
                List<Move> moveList) {

            getSlidingPieceMoves(board, index, side, FILE_DELTAS, RANK_DELTAS, capturesOnly,
                    moveList);
        }

        @Override // Object
        public String toString() {
            return "bishop";
        }

        @Override // PieceType
        public double getPositionBonus(int index) {
            return POSITION_BONUS[index]/100.0;
        }
    };
    public static final PieceType KNIGHT = new PieceType('n', 4, 3) {
        private final int[] POSITION_BONUS = new int[]{
            -50,-40,-30,-30,-30,-30,-40,-50,
            -40,-20,  0,  0,  0,  0,-20,-40,
            -30,  0, 10, 15, 15, 10,  0,-30,
            -30,  5, 15, 20, 20, 15,  5,-30,
            -30,  0, 15, 20, 20, 15,  0,-30,
            -30,  5, 10, 15, 15, 10,  5,-30,
            -40,-20,  0,  5,  5,  0,-20,-40,
            -50,-40,-20,-30,-30,-20,-40,-50,
        };

        // Pair of numbers to provide the eight directions, starting up-right
        // and going clockwise.
        private final int[] FILE_DELTAS = new int[] { 1, 2, 2, 1, -1, -2, -2, -1 };
        private final int[] RANK_DELTAS = new int[] { 2, 1, -1, -2, -2, -1, 1, 2 };

        @Override // PieceType
        public void addMoves(Board board, int index, int side, boolean capturesOnly,
                List<Move> moveList) {

            getJumpingPieceMoves(board, index, side, FILE_DELTAS, RANK_DELTAS, capturesOnly,
                    moveList);
        }

        @Override // Object
        public String toString() {
            return "knight";
        }

        @Override // PieceType
        public double getPositionBonus(int index) {
            return POSITION_BONUS[index]/100.0;
        }
    };
    public static final PieceType ROOK = new PieceType('r', 2, 5) {
        // Pair of numbers to provide the four directions, starting up and going clockwise.
        private final int[] FILE_DELTAS = new int[] { 0, 1, 0, -1 };
        private final int[] RANK_DELTAS = new int[] { 1, 0, -1, 0 };

        @Override // PieceType
        public void addMoves(Board board, int index, int side, boolean capturesOnly,
                List<Move> moveList) {

            getSlidingPieceMoves(board, index, side, FILE_DELTAS, RANK_DELTAS, capturesOnly,
                    moveList);
        }

        @Override // Object
        public String toString() {
            return "rook";
        }
    };
    public static final PieceType QUEEN = new PieceType('q', 1, 9) {
        // Pair of numbers to provide the eight directions, starting up and going clockwise.
        private final int[] FILE_DELTAS = new int[] { 0, 1, 1, 1, 0, -1, -1, -1 };
        private final int[] RANK_DELTAS = new int[] { 1, 1, 0, -1, -1, -1, 0, 1 };

        @Override // PieceType
        public void addMoves(Board board, int index, int side, boolean capturesOnly,
                List<Move> moveList) {

            getSlidingPieceMoves(board, index, side, FILE_DELTAS, RANK_DELTAS, capturesOnly,
                    moveList);
        }

        @Override // Object
        public String toString() {
            return "queen";
        }
    };
    public static final PieceType KING = new PieceType('k', 0, 200) { // Shannon
        // Pair of numbers to provide the eight directions, starting up and going clockwise.
        private final int[] FILE_DELTAS = new int[] { 0, 1, 1, 1, 0, -1, -1, -1 };
        private final int[] RANK_DELTAS = new int[] { 1, 1, 0, -1, -1, -1, 0, 1 };

        @Override // PieceType
        public void addMoves(Board board, int index, int side, boolean capturesOnly,
                List<Move> moveList) {

            getJumpingPieceMoves(board, index, side, FILE_DELTAS, RANK_DELTAS, capturesOnly,
                    moveList);
        }

        @Override // Object
        public String toString() {
            return "king";
        }
    };

    private final char mCharacter;
    private final int mUnicodeOffset;
    private final int mValue;

    private PieceType(char ch, int unicodeOffset, int value) {
        mCharacter = ch;
        mUnicodeOffset = unicodeOffset;
        mValue = value;
    }

    /**
     * Return the character that can be used to represent this piece in ASCII mode,
     * always lower case.
     */
    public char getCharacter() {
        return mCharacter;
    }

    /**
     * Return the offset from 0x2654 for this piece type for white. For example,
     * king is 0 and rook is 2.
     *
     * See http://en.wikipedia.org/wiki/Chess_symbols_in_Unicode
     */
    public int getUnicodeOffset() {
        return mUnicodeOffset;
    }

    /**
     * Return the piece's value (e.g., 3 for bishop, 9 for queen).
     */
    public int getValue() {
        return mValue;
    }

    /**
     * Adds to the list the moves possible by this piece at this position on
     * this side on this board.
     */
    public abstract void addMoves(Board board, int index, int side, boolean capturesOnly,
            List<Move> moveList);

    /**
     * Return the position bonus, as a double where 1 means a pawn value. Index is 
     * from white's point of view. (It assumes this is a white piece.)
     */
    public double getPositionBonus(int index) {
        // Default implementation. Subclass can override.
        return 0;
    }

    /**
     * Adds the moves of a piece that can slide in any number of directions.
     */
    private static void getSlidingPieceMoves(Board board, int index, int side,
            int[] fileDeltas, int[] rankDeltas, boolean capturesOnly, List<Move> moveList) {

        // Try each direction.
        for (int i = 0; i < fileDeltas.length; i++) {
            for (int j = 1; j < Board.SIZE; j++) {
                int otherIndex = board.getRelativeIndex(index,
                        fileDeltas[i]*j, rankDeltas[i]*j);
                if (otherIndex == -1) {
                    // Went off the board.
                    break;
                }
                Piece piece = board.getPiece(otherIndex);
                if (piece == Piece.EMPTY) {
                    // Move to this location.
                    if (!capturesOnly) {
                        moveList.add(Move.make(board, index, otherIndex));
                    }
                } else {
                    if (piece.isOtherSide(side)) {
                        // Capture.
                        moveList.add(Move.make(board, index, otherIndex));
                    }
                    // Reached a piece, stop.
                    break;
                }
            }
        }
    }

    /**
     * Adds the moves of a piece that can jump to a list of locations.
     */
    private static void getJumpingPieceMoves(Board board, int index, int side,
            int[] fileDeltas, int[] rankDeltas, boolean capturesOnly, List<Move> moveList) {

        // Try each direction.
        for (int i = 0; i < fileDeltas.length; i++) {
            int otherIndex = board.getRelativeIndex(index, fileDeltas[i], rankDeltas[i]);
            if (otherIndex != -1) {
                Piece piece = board.getPiece(otherIndex);
                if ((piece == Piece.EMPTY && !capturesOnly) || piece.isOtherSide(side)) {
                    // Move or capture.
                    moveList.add(Move.make(board, index, otherIndex));
                }
            }
        }
    }
}

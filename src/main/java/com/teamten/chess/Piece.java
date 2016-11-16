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

package com.teamten.chess;

import java.util.Arrays;
import java.util.List;

/**
 * Represents a piece of a type and color.
 */
public class Piece {
    public static final Piece EMPTY = new Piece(null, 0);
    public static final Piece WHITE_PAWN = new Piece(PieceType.PAWN, Side.WHITE);
    public static final Piece WHITE_BISHOP = new Piece(PieceType.BISHOP, Side.WHITE);
    public static final Piece WHITE_KNIGHT = new Piece(PieceType.KNIGHT, Side.WHITE);
    public static final Piece WHITE_ROOK = new Piece(PieceType.ROOK, Side.WHITE);
    public static final Piece WHITE_QUEEN = new Piece(PieceType.QUEEN, Side.WHITE);
    public static final Piece WHITE_KING = new Piece(PieceType.KING, Side.WHITE);
    public static final Piece BLACK_PAWN = new Piece(PieceType.PAWN, Side.BLACK);
    public static final Piece BLACK_BISHOP = new Piece(PieceType.BISHOP, Side.BLACK);
    public static final Piece BLACK_KNIGHT = new Piece(PieceType.KNIGHT, Side.BLACK);
    public static final Piece BLACK_ROOK = new Piece(PieceType.ROOK, Side.BLACK);
    public static final Piece BLACK_QUEEN = new Piece(PieceType.QUEEN, Side.BLACK);
    public static final Piece BLACK_KING = new Piece(PieceType.KING, Side.BLACK);
    private static final List<Piece> ALL_PIECES = getPieceList();

    private final PieceType mPieceType;
    private final int mSide;

    private Piece(PieceType pieceType, int side) {
        mPieceType = pieceType;
        mSide = side;
    }

    /**
     * Return the piece type (king, queen, etc) for this piece, or null if an
     * empty square.
     */
    public PieceType getPieceType() {
        return mPieceType;
    }

    /**
     * Return the side (Side.WHITE or Side.BLACK) for this piece, or 0 if an empty square.
     */
    public int getSide() {
        return mSide;
    }

    /**
     * Return the position bonus for this piece at this location.
     */
    public double getPositionBonus(int index) {
        if (mPieceType == null) {
            // Convenience so we can call this on EMPTY.
            return 0;
        } else {
            if (mSide == Side.BLACK) {
                // Piece types are from white's point of view. Note that this depends
                // on the position bonus to be left-right symmetric.
                index = Board.NUM_SQUARES - 1 - index;
            }
            return mPieceType.getPositionBonus(index);
        }
    }

    /**
     * Return the character that can be used to represent this piece in ASCII mode.
     */
    public char getCharacter() {
        return Side.convertCharacter(mSide, mPieceType.getCharacter());
    }

    /**
     * Return the character that can be used to represent this piece in Algebraic Notation.
     */
    public char getAlgebraicCharacter() {
        return Character.toUpperCase(mPieceType.getCharacter());
    }

    /**
     * Returns the char for the Unicode character.
     *
     * See http://en.wikipedia.org/wiki/Chess_symbols_in_Unicode
     */
    public char getUnicodeCharacter() {
        return (char) (0x2654 + mPieceType.getUnicodeOffset() + Side.getUnicodeOffset(mSide));
    }

    /**
     * Same as getUnicodeCharacter() but as an HTML entity.
     */
    public String getHtmlCharacter() {
        return "&#" + (int) getUnicodeCharacter() + ";";
    }

    /**
     * Adds the moves possible by this piece at this position on this board.
     */
    public void addMoves(Board board, int index, boolean capturesOnly, List<Move> moveList) {
        mPieceType.addMoves(board, index, mSide, capturesOnly, moveList);
    }

    /**
     * Return whether this piece is from the other side as the specified side. Empty
     * squares are never on the other side.
     */
    public boolean isOtherSide(int otherSide) {
        return mPieceType != null && mSide != otherSide;
    }

    @Override // Object
    public String toString() {
        if (mPieceType == null) {
            return "empty";
        } else {
            return Side.toString(mSide) + " " + mPieceType;
        }
    }

    /**
     * Return a list of all pieces we know, not including EMPTY.
     */
    private static List<Piece> getPieceList() {
        return Arrays.asList(
            WHITE_PAWN,
            WHITE_BISHOP,
            WHITE_KNIGHT,
            WHITE_ROOK,
            WHITE_QUEEN,
            WHITE_KING,
            BLACK_PAWN,
            BLACK_BISHOP,
            BLACK_KNIGHT,
            BLACK_ROOK,
            BLACK_QUEEN,
            BLACK_KING);
    }

    /**
     * Return the piece that would have returned this from its getCharacter()
     * method.
     *
     * @throws IllegalArgumentException if none is found.
     */
    public static Piece getPieceForCharacter(char ch) {
        for (Piece piece : ALL_PIECES) {
            if (piece.getCharacter() == ch) {
                return piece;
            }
        }

        throw new IllegalArgumentException("no piece for \"" + ch + "\"");
    }

    /**
     * Return the piece for this piece type and side.
     */
    public static Piece getPieceForTypeAndSide(PieceType pieceType, int side) {
        for (Piece piece : ALL_PIECES) {
            if (piece.getPieceType() == pieceType && piece.getSide() == side) {
                return piece;
            }
        }

        throw new IllegalArgumentException("no piece for "
                + Side.toString(side) + " " + pieceType);
    }
}

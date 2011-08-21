// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/**
 * Represents a chess board and the pieces on it.
 */
public class Board {
    public static final int SIZE = 8;
    public static final int NUM_SQUARES = SIZE*SIZE;
    /**
     * Which directions the queen can go.
     */
    private static final int[] QUEEN_FILE_DELTAS = new int[] { 0, 1, 1, 1, 0, -1, -1, -1 };
    private static final int[] QUEEN_RANK_DELTAS = new int[] { 1, 1, 0, -1, -1, -1, 0, 1 };
    /**
     * Which directions the knight can go.
     */
    private static final int[] KNIGHT_FILE_DELTAS = new int[] { 1, 2, 2, 1, -1, -2, -2, -1 };
    private static final int[] KNIGHT_RANK_DELTAS = new int[] { 2, 1, -1, -2, -2, -1, 1, 2 };
    private static final Random RANDOM = new Random();

    /**
     * SIZE*SIZE array, with 0 being black's queen-side rook, 1 black's
     * queen-side knight, and 63 being white's king-side rook.
     */
    private final Piece[] mSquare = new Piece[SIZE*SIZE];

    /**
     * The index of each king, indexed by side, or -1 if none.
     */
    private final int[] mKingIndex = new int[] { -1, -1 };

    /**
     * Whose turn it is.
     */
    private int mSide;

    /**
     * Number of non-pawn pieces for each side.
     */
    private final int[] mNumPieces = new int[2];

    /**
     * Clears the board.
     */
    public void initializeEmpty() {
        mSide = Side.WHITE;

        for (int i = 0; i < SIZE*SIZE; i++) {
            setPiece(i, Piece.EMPTY);
        }
    }

    /**
     * Put the pawns on the board at their traditional place.
     */
    private void initializePawns() {
        for (int file = 1; file <= SIZE; file++) {
            setPiece(getIndex(file, 2), Piece.WHITE_PAWN);
            setPiece(getIndex(file, 7), Piece.BLACK_PAWN);
        }
    }

    /**
     * Set the pieces up for traditional chess.
     */
    public void initializeTraditionalChess() {
        // Clear out board.
        initializeEmpty();

        // Pawns.
        initializePawns();

        // Pieces.
        setPiece(getIndex(1, 1), Piece.WHITE_ROOK);
        setPiece(getIndex(2, 1), Piece.WHITE_KNIGHT);
        setPiece(getIndex(3, 1), Piece.WHITE_BISHOP);
        setPiece(getIndex(4, 1), Piece.WHITE_QUEEN);
        setPiece(getIndex(5, 1), Piece.WHITE_KING);
        setPiece(getIndex(6, 1), Piece.WHITE_BISHOP);
        setPiece(getIndex(7, 1), Piece.WHITE_KNIGHT);
        setPiece(getIndex(8, 1), Piece.WHITE_ROOK);
        setPiece(getIndex(1, 8), Piece.BLACK_ROOK);
        setPiece(getIndex(2, 8), Piece.BLACK_KNIGHT);
        setPiece(getIndex(3, 8), Piece.BLACK_BISHOP);
        setPiece(getIndex(4, 8), Piece.BLACK_QUEEN);
        setPiece(getIndex(5, 8), Piece.BLACK_KING);
        setPiece(getIndex(6, 8), Piece.BLACK_BISHOP);
        setPiece(getIndex(7, 8), Piece.BLACK_KNIGHT);
        setPiece(getIndex(8, 8), Piece.BLACK_ROOK);
    }

    /**
     * Set the pieces up for chess 960.
     */
    public void initializeChess960() {
        // Clear out board.
        initializeEmpty();

        // Pawns.
        initializePawns();

        // Bishops. This forces them on opposite colors.
        int pos = 1 + RANDOM.nextInt(4)*2;
        setPiece(getIndex(pos, 1), Piece.WHITE_BISHOP);
        setPiece(getIndex(pos, 8), Piece.BLACK_BISHOP);

        pos = 2 + RANDOM.nextInt(4)*2;
        setPiece(getIndex(pos, 1), Piece.WHITE_BISHOP);
        setPiece(getIndex(pos, 8), Piece.BLACK_BISHOP);

        // Queen and knights.
        place960Piece(PieceType.QUEEN, RANDOM.nextInt(6) + 1);
        place960Piece(PieceType.KNIGHT, RANDOM.nextInt(5) + 1);
        place960Piece(PieceType.KNIGHT, RANDOM.nextInt(4) + 1);

        // King between the rooks.
        place960Piece(PieceType.ROOK, 3);
        place960Piece(PieceType.KING, 2);
        place960Piece(PieceType.ROOK, 1);
    }

    /**
     * Places the white and black pieces for this piece type in the spot-th
     * first empty spot, where the first is 1.
     */
    private void place960Piece(PieceType pieceType, int spot) {
        int pos = 0;

        do {
            pos++;
            // Find the next empty square.
            while (getPiece(getIndex(pos, 1)) != Piece.EMPTY) {
                pos++;
            }
            spot--;
        } while (spot > 0);

        setPiece(getIndex(pos, 1), Piece.getPieceForTypeAndSide(pieceType, Side.WHITE));
        setPiece(getIndex(pos, 8), Piece.getPieceForTypeAndSide(pieceType, Side.BLACK));
    }

    /**
     * Return the index into the SIZE*SIZE array given a rank and file.
     *
     * @param file the file or column, 1 to 8. 1 is the queen-side rook.
     * @param rank the rank or row, 1 to 8. 1 is white's back row.
     */
    public static int getIndex(int file, int rank) {
        return (file - 1) + (SIZE - rank)*SIZE;
    }

    /**
     * Like getIndex(), but offset by the delta rank and file. Returns -1 if
     * off-board.
     */
    public static int getRelativeIndex(int index, int deltaFile, int deltaRank) {
        int file = getFile(index) + deltaFile;
        int rank = getRank(index) + deltaRank;

        if (file < 1 || file > SIZE || rank < 1 || rank > SIZE) {
            return -1;
        }

        return getIndex(file, rank);
    }

    /**
     * Return the file (1 to SIZE) for this index.
     */
    public static int getFile(int index) {
        return index % SIZE + 1;
    }

    /**
     * Return the rank (1 to SIZE) for this index.
     */
    public static int getRank(int index) {
        return SIZE - index / SIZE;
    }

    /**
     * Return the letter for the file of this index.
     */
    public static String getFileLetter(int index) {
        int file = getFile(index);
        return Character.toString((char) ('a' + file - 1));
    }

    /**
     * Return the digit for the rank of this index.
     */
    public static String getRankDigit(int index) {
        int rank = getRank(index);
        return Integer.toString(rank);
    }

    /**
     * Return a position (such as "a1") for an index.
     */
    public static String getPosition(int index) {
        return getFileLetter(index) + getRankDigit(index);
    }

    /**
     * Return an index given a position like "e2".
     *
     * @throws IllegalArgumentException if the position is poorly formatted.
     */
    public static int fromPosition(String position) {
        if (position.length() != 2) {
            throw new IllegalArgumentException("position is invalid: " + position);
        }

        int file = (int) (position.charAt(0) - 'a' + 1);
        int rank = (int) (position.charAt(1) - '0');

        if (file < 1 || file > 8 || rank < 1 || rank > 8) {
            throw new IllegalArgumentException("position is invalid: " + position);
        }

        return getIndex(file, rank);
    }

    /**
     * Return whether the specified index is one of the four center squares.
     */
    public static boolean isCenterSquare(int index) {
        return index == 27
            || index == 27 + 1
            || index == 27 + 8
            || index == 27 + 8 + 1;
    }

    /**
     * Return the side whose turn it is.
     */
    public int getSide() {
        return mSide;
    }

    /**
     * Set the side whose turn it is.
     */
    public void setSide(int side) {
        mSide = side;
    }

    /**
     * Swap the side whose turn it is, turning the new side.
     */
    public int swapSides() {
        mSide = Side.getOtherSide(mSide);
        return mSide;
    }

    /**
     * Sets the piece at index, or EMPTY to empty the square.
     */
    public void setPiece(int index, Piece piece) {
        Piece previousPiece = mSquare[index];
        mSquare[index] = piece;

        // Forget previous king location.
        if (previousPiece != null && previousPiece != Piece.EMPTY) {
            if (previousPiece.getPieceType() == PieceType.KING) {
                mKingIndex[previousPiece.getSide()] = -1;
            }
            if (previousPiece.getPieceType() != PieceType.PAWN) {
                mNumPieces[previousPiece.getSide()] -= 1;
            }
        }

        if (piece != Piece.EMPTY) {
            // Remember new king location.
            if (piece.getPieceType() == PieceType.KING) {
                mKingIndex[piece.getSide()] = index;
            }
            if (piece.getPieceType() != PieceType.PAWN) {
                mNumPieces[piece.getSide()] += 1;
            }
        }
    }

    /**
     * Return the piece at index, or EMPTY if the square is empty.
     */
    public Piece getPiece(int index) {
        return mSquare[index];
    }

    /**
     * Moves a piece from one spot to another, returning the piece that
     * was at the destination square, or EMPTY if none.
     */
    public Piece movePiece(int fromIndex, int toIndex) {
        Piece previousToPiece = getPiece(toIndex);
        Piece previousFromPiece = getPiece(fromIndex);

        // Always remove the piece first, then add it. Otherwise the board temporarily
        // has two of these pieces, which messed up bookkeeping in mKingIndex.
        setPiece(fromIndex, Piece.EMPTY);
        setPiece(toIndex, previousFromPiece);

        return previousToPiece;
    }

    /**
     * Return the number of (non-pawn) pieces for this side.
     */
    public int getNumPieces(int side) {
        return mNumPieces[side];
    }

    /**
     * Return the total number of non-pawn pieces on the board.
     */
    public int getTotalPieces() {
        return getNumPieces(Side.WHITE) + getNumPieces(Side.BLACK);
    }

    /**
     * Return whether the game is in the end game.
     */
    public boolean isEndGame() {
        // This includes both kings. The definition here is a bit sloppy, but we consider
        // it endgame if we have fewer than 4 non-king non-pawn pieces left altogether.
        return getTotalPieces() <= 6;
    }

    /**
     * If the given square is attacked by the given side, returns the index of (any)
     * attacking piece. Otherwise returns -1.
     */
    public int getAttackingPieceIndex(int index, int attackingSide) {
        // Check every direction for queen (includes rook, bishop, king, and pawn).
        for (int i = 0; i < QUEEN_FILE_DELTAS.length; i++) {
            // Check every distance.
            for (int distance = 1; distance < SIZE; distance++) {
                int fileDelta = QUEEN_FILE_DELTAS[i];
                int rankDelta = QUEEN_RANK_DELTAS[i];
                int otherIndex = getRelativeIndex(index, distance*fileDelta, distance*rankDelta);
                if (otherIndex == -1) {
                    // Off board.
                    break;
                }

                Piece piece = getPiece(otherIndex);
                if (piece != Piece.EMPTY) {
                    if (piece.getSide() == attackingSide) {
                        if (piece.getPieceType() == PieceType.QUEEN) {
                            return otherIndex;
                        }
                        if (piece.getPieceType() == PieceType.KING && distance == 1) {
                            return otherIndex;
                        }

                        if (fileDelta == 0 || rankDelta == 0) {
                            // Orthogonal.
                            if (piece.getPieceType() == PieceType.ROOK) {
                                return otherIndex;
                            }
                        } else {
                            // Diagonal.
                            if (piece.getPieceType() == PieceType.BISHOP) {
                                return otherIndex;
                            }

                            if (piece.getPieceType() == PieceType.PAWN && distance == 1) {
                                if (attackingSide == Side.WHITE) {
                                    if (otherIndex > index) {
                                        return otherIndex;
                                    }
                                } else {
                                    if (otherIndex < index) {
                                        return otherIndex;
                                    }
                                }
                            }
                        }
                    }
                    // Stop this direction anyway, we've hit a piece.
                    break;
                }
            }
        }

        // Check knights.
        for (int i = 0; i < KNIGHT_FILE_DELTAS.length; i++) {
            int fileDelta = KNIGHT_FILE_DELTAS[i];
            int rankDelta = KNIGHT_RANK_DELTAS[i];
            int otherIndex = getRelativeIndex(index, fileDelta, rankDelta);
            // Check if off board.
            if (otherIndex != -1) {
                Piece piece = getPiece(otherIndex);
                if (piece != Piece.EMPTY
                        && piece.getPieceType() == PieceType.KNIGHT
                        && piece.getSide() == attackingSide) {

                    return otherIndex;
                }
            }
        }

        return -1;
    }

    /**
     * If the king of the specified side is in check, return (any) index of an attacking
     * piece. Otherwise return -1.
     */
    public int getCheckIndex(int side) {
        // Find the king.
        int kingIndex = mKingIndex[side];

        if (kingIndex != -1) {
            Piece piece = getPiece(kingIndex);
            if (piece.getPieceType() != PieceType.KING || piece.getSide() != side) {
                throw new IllegalStateException("King isn't where it should be");
            }

            return getAttackingPieceIndex(kingIndex, Side.getOtherSide(side));
        }

        return -1;
    }

    /**
     * Verifies whether this move is legal. This only checks for things like
     * whether the king is in check as a result of this move. It does not check
     * to see if the piece can actually move this way. Use isMoveValid() for
     * that.
     *
     * @return the index of a piece that's making this move not legal, or -1
     * if the move is legal.
     */
    public int verifyMoveLegal(Move move) {
        move.applyMove(this);
        int attackingIndex = getCheckIndex(move.getMovingPiece().getSide());
        move.applyInverseMove(this);

        return attackingIndex;
    }

    /**
     * This fully checks this move for validity: the side whose turn it is to move,
     * the motion of the piece, and the legality (in check, etc.). This is a
     * relatively slow method. Call it at user-interface speeds.
     *
     * @throws IllegalMoveException if the move is not valid. The exception includes
     * a human-readable string explaining why it's not valid and optionally an
     * index pointing at a piece that's causing a check.
     */
    public void verifyMoveValid(Move move) throws IllegalMoveException {
        Piece piece = move.getMovingPiece();

        if (piece == Piece.EMPTY) {
            throw new IllegalMoveException("That square is empty.");
        }

        // See if it's the side's turn to move.
        if (piece.getSide() != mSide) {
            throw new IllegalMoveException("It's not "
                    + Side.toString(piece.getSide()) + "'s turn to move.");
        }

        // See if that piece can move that way.
        List<Move> moveList = generateAllMoves(mSide, false);
        boolean inMoveList = false;
        for (Move validMove : moveList) {
            if (validMove.equals(move)) {
                inMoveList = true;
                break;
            }
        }
        if (!inMoveList) {
            throw new IllegalMoveException("That's not a valid move for a "
                    + piece.getPieceType() + ".");
        }

        // See if it puts the king in check.
        int checkIndex = verifyMoveLegal(move);
        if (checkIndex != -1) {
            throw new IllegalMoveException("That would leave "
                    + Side.toString(mSide) + "'s king in check from "
                    + getPosition(checkIndex) + ".", checkIndex);
        }
    }

    /**
     * Remove any move that leaves the board in an illegal state (e.g., king in check).
     */
    public void removeIllegalMoves(List<Move> moveList) {
        Iterator<Move> itr = moveList.iterator();

        // Try each move to see if it would result in a bad state.
        while (itr.hasNext()) {
            Move move = itr.next();
            if (verifyMoveLegal(move) != -1) {
                itr.remove();
            }
        }
    }

    /**
     * Parse a move like "e2-e4" in the context of this board.
     *
     * @throws IllegalArgumentException if the move can't be parsed or is illegal.
     * The exception message is human-readable.
     */
    public Move parseMove(String moveString) throws IllegalMoveException {
        if (moveString.length() != 5 || moveString.charAt(2) != '-') {
            throw new IllegalArgumentException("Move is badly formatted: " + moveString);
        }

        int fromIndex = fromPosition(moveString.substring(0, 2));
        int toIndex = fromPosition(moveString.substring(3, 5));
        Piece movingPiece = getPiece(fromIndex);
        int rank = getRank(toIndex);

        Move move;

        if (movingPiece.getPieceType() == PieceType.PAWN &&
                ((rank == 1 && mSide == Side.BLACK)
                 || (rank == 8 && mSide == Side.WHITE))) {

            move = Move.makePromotion(this, fromIndex, toIndex,
                    Piece.getPieceForTypeAndSide(PieceType.QUEEN, mSide));
        } else {
            move = Move.make(this, fromIndex, toIndex);
        }

        // Throws if not valid.
        verifyMoveValid(move);

        return move;
    }

    /**
     * Generates all moves for the side whose turn it is, but does not remove moves
     * that would leave us in check.
     */
    public List<Move> generateAllMoves(int side, boolean capturesOnly) {
        List<Move> moveList = new ArrayList<Move>();

        for (int index = 0; index < SIZE*SIZE; index++) {
            Piece piece = getPiece(index);
            if (piece != Piece.EMPTY && piece.getSide() == side) {
                piece.addMoves(this, index, capturesOnly, moveList);
            }
        }

        return moveList;
    }

    /**
     * Prints the board to the terminal with each line having the specified indent.
     */
    public void print(String indent) {
        for (int rank = SIZE; rank >= 1; rank--) {
            System.out.print(indent);
            for (int file = 1; file <= SIZE; file++) {
                Piece piece = getPiece(getIndex(file, rank));

                char ch;
                if (piece == Piece.EMPTY) {
                    if ((file + rank) % 2 == 0) {
                        ch = ' ';
                    } else {
                        ch = '.';
                    }
                } else {
                    ch = piece.getCharacter();
                }

                System.out.print(ch);
                System.out.print(' ');
            }
            System.out.println();
        }
        System.out.println("---------------");
    }

    /**
     * Serialize the board to a string. Deserialize later with deserialize(). The
     * string contains only URL-safe characters (A-Z, a-z, digits, and periods).
     */
    public String serialize() {
        StringBuilder builder = new StringBuilder();

        for (int i = 0; i < NUM_SQUARES; i++) {
            Piece piece = mSquare[i];
            if (piece == Piece.EMPTY) {
                builder.append('.');
            } else {
                builder.append(piece.getCharacter());
            }
        }
        builder.append(getSide());

        return builder.toString();
    }

    /**
     * Deserialize a board from the string created by serialize().
     */
    public static Board deserialize(String str) {
        Board board = new Board();

        for (int i = 0; i < NUM_SQUARES; i++) {
            char ch = str.charAt(i);
            Piece piece;

            if (ch == '.') {
                piece = Piece.EMPTY;
            } else {
                piece = Piece.getPieceForCharacter(ch);
            }

            board.setPiece(i, piece);
        }

        board.mSide = (int) (str.charAt(NUM_SQUARES) - '0');

        return board;
    }

    /**
     * Parse a board from ASCII. The input in row-major order, from rank 8 to 1
     * (black side to white side). Whitespace is ignored. Blank squares must
     * contain a period (.).  White characters are upper case, black lower
     * case. See PieceType for the letters.
     *
     * @throws IllegalArgumentException if the board can't be parsed.
     */
    public static Board parse(String str) {
        Board board = new Board();

        // The specification for the layout of "str" matches the index
        // we use, so just fill out each piece in index order.
        int index = 0;

        for (int i = 0; i < str.length(); i++) {
            char ch = str.charAt(i);

            if (!Character.isWhitespace(ch)) {
                Piece piece;

                if (ch == '.') {
                    piece = Piece.EMPTY;
                } else {
                    piece = Piece.getPieceForCharacter(ch);
                }

                board.setPiece(index, piece);
                index++;
            }
        }

        // Quick sanity check.
        if (index != SIZE*SIZE) {
            throw new IllegalArgumentException(
                    String.format("Invalid number of squares (%d instead of %d",
                        index, SIZE*SIZE));
        }

        return board;
    }
}

// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess;

import java.io.FileNotFoundException;
import java.io.PrintStream;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.Stack;

/**
 * Records an entire game.
 */
public class Game {
    private static final SimpleDateFormat PGN_DATE_FORMAT = new SimpleDateFormat("yyyy.MM.dd");
    private final Stack<Move> mMoveList = new Stack<Move>();
    private final Stack<Move> mRedoList = new Stack<Move>();
    private final Board mBoard;

    public Game(Board board) {
        mBoard = board;
    }

    /**
     * Return the board this game is associated with.
     */
    public Board getBoard() {
        return mBoard;
    }

    /**
     * Returns the number of moves in this game.
     */
    public int getMoveCount() {
        return mMoveList.size();
    }

    /**
     * Get the move by index.
     */
    public Move getMove(int index) {
        return mMoveList.get(index);
    }

    /**
     * Adds a new move at the end of this game.
     */
    public void addMove(Move move) {
        // Set moves since capture if unknown, since that would mean that
        // it's not itself a capture or pawn move.
        if (move.getMovesSinceCapture() == -1) {
            int movesSinceCapture;

            if (mMoveList.empty()) {
                movesSinceCapture = 1;
            } else {
                movesSinceCapture = mMoveList.peek().getMovesSinceCapture() + 1;
            }

            move.setMovesSinceCapture(movesSinceCapture);
        }

        mRedoList.clear();
        mMoveList.push(move);
        move.applyMove(mBoard);
        mBoard.swapSides();
        if (mBoard.getCheckIndex(mBoard.getSide()) != -1) {
            move.setCheck(true);
        }
    }

    /**
     * Removes the last move in this game.
     */
    public void undoMove() {
        if (getMoveCount() == 0) {
            throw new IllegalStateException("Can't undo empty game");
        }

        mBoard.swapSides();
        Move move = mMoveList.pop();
        move.applyInverseMove(mBoard);
        mRedoList.push(move);
    }

    /**
     * Re-plays a previously-undone move. Does nothing if the redo stack is empty.
     */
    public void redoMove() {
        if (!mRedoList.empty()) {
            mBoard.swapSides();
            Move move = mRedoList.pop();
            move.applyMove(mBoard);
            mMoveList.push(move);
        }
    }

    /**
     * Serialize the game to a string. Deserialize later with deserialize(). The
     * string contains only URL-safe characters (A-Z, a-z, digits, periods, commas,
     * and hyphens).
     */
    public String serialize() {
        StringBuilder builder = new StringBuilder();

        builder.append(mBoard.serialize());

        for (Move move : mMoveList) {
            builder.append(',');
            builder.append(move.serialize());
        }
        builder.append(',');
        builder.append("-");
        for (Move move : mRedoList) {
            builder.append(',');
            builder.append(move.serialize());
        }

        return builder.toString();
    }

    /**
     * Deserialize a game from the string created by serialize().
     */
    public static Game deserialize(String str) {
        String[] fields = str.split(",");

        Board board = Board.deserialize(fields[0]);

        Game game = new Game(board);

        boolean redo = false;
        for (int i = 1; i < fields.length; i++) {
            if (fields[i].equals("-")) {
                redo = true;
            } else {
                Move move = Move.deserialize(fields[i]);
                if (redo) {
                    game.mRedoList.push(move);
                } else {
                    game.mMoveList.push(move);
                }
            }
        }

        return game;
    }

    public void writePgn(String filename, int winningSide, int round) {
        PrintStream w;

        try {
            w = new PrintStream(filename);
        } catch (FileNotFoundException e) {
            System.out.println("Cannot write to " + filename + " (" + e + ")");
            return;
        }

        writePgn(w, winningSide, round);

        w.close();
    }

    public void writePgn(PrintStream w, int winningSide, int round) {
        w.printf("[Event \"Private match\"]%n");
        w.printf("[Site \"San Francisco, CA USA\"]%n");
        w.printf("[Date \"%s\"]%n", PGN_DATE_FORMAT.format(new Date()));
        w.printf("[Round \"%d\"]%n", round);
        w.printf("[White \"Computer\"]%n");
        w.printf("[Black \"Computer\"]%n");
        w.printf("[Result \"%s\"]%n", Side.toPgnNotation(winningSide));
        w.printf("%n");

        int ply = 0;
        for (Move move : mMoveList) {
            if (ply % 2 == 0) {
                w.printf("%d. ", ply / 2 + 1);
            }
            w.printf("%s ", move);
            ply++;
        }

        if (winningSide != Side.IN_PROGRESS) {
            w.printf("%s", Side.toPgnNotation(winningSide));
        }

        w.printf("%n%n");
    }

    /**
     * Whether this game can be drawn from the 50-rule-move.
     *
     * http://en.wikipedia.org/wiki/Fifty-move_rule
     */
    public boolean isDrawFrom50MoveRule() {
        // 100 because we're actually counting plys.
        return !mMoveList.empty() && mMoveList.peek().getMovesSinceCapture() > 100;
    }
}

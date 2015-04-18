// Copyright 2015 Lawrence Kesteloot

package com.teamten.chess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

/**
 * Plays two versions of our chess program against one another.
 */
public class Match {
    private final Player[] mPlayers;
    private final Board mBoard;
    private final Game mGame;

    private Match(String[] gitRevisions) {
        mPlayers = new Player[2];
        mBoard = new Board();
        mGame = new Game(mBoard);
        mBoard.initializeTraditionalChess();

        for (int i = 0; i < 2; i++) {
            mPlayers[i] = new Player(gitRevisions[i]);
        }
    }

    private void start() {
        // Set up.
        for (int i = 0; i < 2; i++) {
            boolean successful = mPlayers[i].start();
            if (!successful) {
                return;
            }

            successful = mPlayers[i].startUci();
            if (!successful) {
                return;
            }

            mPlayers[i].newGame();
        }

        // Play game.
        int sideToPlay = 0;
        while (true) {
            Move move = mPlayers[sideToPlay].computeMove(mGame);
            if (move == null) {
                break;
            }
            mGame.addMove(move);
            mBoard.print(System.out, "", move);

            sideToPlay = 1 - sideToPlay;
        }

        // Shut down.
        for (int i = 0; i < 2; i++) {
            mPlayers[i].quit();
        }
    }

    // --------------------------------------------------------------------------------

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Must specify two git revisions or \"current\".");
            System.exit(1);
        }

        new Match(args).start();
    }

    // --------------------------------------------------------------------------------

    private static class Player {
        private final String mGitRevision;
        private final File mDir;
        private Process mProcess;
        private PrintWriter mWriter;
        private BufferedReader mReader;

        public Player(String gitRevision) {
            mGitRevision = gitRevision;
            mDir = new File("/Users/lk/teamten/java/test");
        }

        public boolean start() {
            try {
                mProcess = new ProcessBuilder("./chessuci").directory(mDir).start();
            } catch (IOException e) {
                System.err.println("Cannot start " + mGitRevision + ": " + e);
                return false;
            }

            mWriter = new PrintWriter(mProcess.getOutputStream(), true);
            mReader = new BufferedReader(new InputStreamReader(mProcess.getInputStream()));

            return true;
        }

        public boolean startUci() {
            mWriter.println("uci");
            while (true) {
                String line = readLine();
                if (line == null) {
                    return false;
                }

                System.err.println(line);
                if (line.equals("uciok")) {
                    break;
                }
            }

            return true;
        }

        public void newGame() {
            mWriter.println("ucinewgame");
            waitForReady();
        }

        public void waitForReady() {
            mWriter.println("isready");
            while (true) {
                String line = readLine();
                if (line == null) {
                    return;
                }

                System.err.println(line);
                if (line.equals("readyok")) {
                    break;
                }
            }
        }

        public Move computeMove(Game game) {
            // Send current game.
            mWriter.print("position startpos");
            if (game.getMoveCount() != 0) {
                mWriter.print(" moves");
                for (int i = 0; i < game.getMoveCount(); i++) {
                    Move move = game.getMove(i);
                    mWriter.print(" " + move.getLongAlgebraicNotation());
                }
            }
            mWriter.println();

            // Ask for move.
            mWriter.println("go movetime 1000");

            // Wait for move.
            while (true) {
                String line = readLine();
                if (line == null) {
                    return null;
                }

                System.err.println(line);
                if (line.startsWith("bestmove ")) {
                    line = line.substring(9);
                    Move move = Move.parseLongAlgebraicNotation(game.getBoard(), line);
                    return move;
                }
            }
        }

        public void quit() {
            mWriter.println("quit");
            finish();
        }

        public void finish() {
            if (mProcess != null) {
                System.err.println("Finishing " + mGitRevision);
                try {
                    mProcess.waitFor();
                } catch (InterruptedException e) {
                    // Ignore.
                }
            }

            mProcess = null;
            // mWriter = null;
            // mReader = null;
        }

        private String readLine() {
            String line;

            try {
                line = mReader.readLine();
            } catch (IOException e) {
                System.err.println("Got exception reading line: " + e);
                line = null;
            }

            if (line == null) {
                finish();
            }

            return line;
        }
    }
}

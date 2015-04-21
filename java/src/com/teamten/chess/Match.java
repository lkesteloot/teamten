// Copyright 2015 Lawrence Kesteloot

package com.teamten.chess;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.List;

/**
 * Plays two versions of our chess program against one another.
 */
public class Match {
    private final String[] mGitRevisions;

    private Match(String[] gitRevisions) {
        mGitRevisions = gitRevisions;
    }

    private void start() {
        Player[] players = new Player[2];

        for (int i = 0; i < 2; i++) {
            players[i] = new Player(mGitRevisions[i]);
        }

        // Set up.
        for (int i = 0; i < 2; i++) {
            boolean successful = players[i].start();
            if (!successful) {
                return;
            }

            successful = players[i].startUci();
            if (!successful) {
                return;
            }
        }

        // Play an even number of games, keeping track of statistics.
        long beforeMatch = System.currentTimeMillis();
        int GAME_COUNT = 50;
        for (int i = 0; i < GAME_COUNT; i++) {
            System.err.println("-----------------------------------------------------------------");
            System.err.printf("Starting game %d of %d.%n", i + 1, GAME_COUNT);
            boolean swap = (i % 2) != 0;
            Player[] playingPlayers = swap ? getSwapped(players) : players;

            long beforeGame = System.currentTimeMillis();
            int winner = playGame(playingPlayers);
            long afterGame = System.currentTimeMillis();
            if (winner == -1) {
                System.err.println("Game is a draw");
                playingPlayers[0].scoreDraw(Side.WHITE);
                playingPlayers[1].scoreDraw(Side.BLACK);
            } else {
                System.err.println(Side.toString(winner) + " wins");
                int loser = Side.getOtherSide(winner);
                playingPlayers[winner].scoreWin(winner);
                playingPlayers[loser].scoreLoss(loser);
            }
            System.err.printf("Game took %s.%n", getElapsedTime(beforeGame, afterGame));
        }
        long afterMatch = System.currentTimeMillis();

        // Shut down.
        for (int i = 0; i < 2; i++) {
            System.err.printf("Score for player %d: %.1f (%s)%n", i, players[i].getScore(),
                    players[i].getScoreBreakdown());
            players[i].quit();
        }
        System.err.printf("Match of %d games took %s.%n",
                GAME_COUNT, getElapsedTime(beforeMatch, afterMatch));
    }

    private static String getElapsedTime(long before, long after) {
        long seconds = (after - before + 500) / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds -= minutes * 60;
        minutes -= hours * 60;

        return String.format("%d:%02d:%02d", hours, minutes, seconds);
    }

    /**
     * Returns the winning side, or -1 if it's a draw.
     */
    private int playGame(Player[] players) {
        Board board = new Board();
        Game game = new Game(board);

        // Set up.
        board.initializeTraditionalChess();
        for (int i = 0; i < 2; i++) {
            players[i].newGame();
        }

        // Play game.
        int sideToPlay = 0;
        while (true) {
            // Generate all legal moves for this side.
            List<Move> moveList = board.generateAllLegalMoves(sideToPlay);

            // If we have no legal moves left, then it's either stalemate or checkmate.
            if (moveList.isEmpty()) {
                if (board.getCheckIndex(sideToPlay) != -1) {
                    // Checkmate.
                    return Side.getOtherSide(sideToPlay);
                } else {
                    // Stalemate.
                    return -1;
                }
            }

            // See if it's a draw.
            if (game.isDrawFrom50MoveRule()) {
                return -1;
            }

            Move move = players[sideToPlay].computeMove(game);
            if (move == null) {
                // This is an error. We should ourselves have detected that there
                // were no moves to play.
                throw new IllegalStateException("Player return no move");
            }
            game.addMove(move);
            board.print(System.out, "", move);

            sideToPlay = 1 - sideToPlay;
        }
    }

    /**
     * Return a new array with the two elements swapped.
     */
    private static Player[] getSwapped(Player[] players) {
        return new Player[]{ players[1], players[0] };
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
        private int[] mDrawCount = new int[2];
        private int[] mWinCount = new int[2];
        private int[] mLossCount = new int[2];

        public Player(String gitRevision) {
            mGitRevision = gitRevision;
            mDir = new File("/Users/lk/teamten/java/test");
        }

        public double getScore() {
            return mWinCount[Side.WHITE] + mWinCount[Side.BLACK] +
                (mDrawCount[Side.WHITE] + mDrawCount[Side.BLACK])*0.5;
        }

        public void scoreDraw(int asSide) {
            mDrawCount[asSide]++;
        }

        public void scoreWin(int asSide) {
            mWinCount[asSide]++;
        }

        public void scoreLoss(int asSide) {
            mLossCount[asSide]++;
        }

        public String getScoreBreakdown() {
            return String.format("Wins: %d/%d, Losses: %d/%d, Draws: %d/%d",
                    mWinCount[Side.WHITE],
                    mWinCount[Side.BLACK],
                    mLossCount[Side.WHITE],
                    mLossCount[Side.BLACK],
                    mDrawCount[Side.WHITE],
                    mDrawCount[Side.BLACK]);
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
            mWriter.println("go movetime 2000");

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

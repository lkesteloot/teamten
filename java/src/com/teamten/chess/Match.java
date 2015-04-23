// Copyright 2015 Lawrence Kesteloot

package com.teamten.chess;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;

import java.text.SimpleDateFormat;

import java.util.Date;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Plays two versions of our chess program against one another.
 */
public class Match {
    private static final File TMP_DIRECTORY = new File("/tmp/chess");
    private static final SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd.HH-mm");
    private static final int GAME_COUNT = 10;
    private static final long MOVE_TIME = 5000;

    private void start(String[] gitRevisions) {
        PrintStream log;
        try {
            String logFilename = "match." + LOG_DATE_FORMAT.format(new Date()) + ".log";
            log = new PrintStream(new FileOutputStream(logFilename), true);
        } catch (IOException e) {
            System.err.println("Cannot create log file: " + e);
            return;
        }
        log(log, "%s%n", new Date());

        // Delete our temporary directory.
        FileUtils.deleteQuietly(TMP_DIRECTORY);
        TMP_DIRECTORY.mkdirs();

        Player[] players = new Player[2];

        for (int i = 0; i < 2; i++) {
            log(log, "Player %d: %s%n", i, gitRevisions[i]);
            players[i] = new Player(TMP_DIRECTORY, gitRevisions[i]);
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
        for (int i = 0; i < GAME_COUNT; i++) {
            log(log, "-----------------------------------------------------------------%n");
            log(log, "Starting game %d of %d.%n", i + 1, GAME_COUNT);
            boolean swap = (i % 2) != 0;
            Player[] playingPlayers = swap ? getSwapped(players) : players;

            long beforeGame = System.currentTimeMillis();
            int winner = playGame(playingPlayers, log, i + 1);
            long afterGame = System.currentTimeMillis();
            if (winner == Side.DRAW) {
                log(log, "Game is a draw.%n");
                playingPlayers[0].scoreDraw(Side.WHITE);
                playingPlayers[1].scoreDraw(Side.BLACK);
            } else {
                log(log, Side.toString(winner) + " wins.%n");
                int loser = Side.getOtherSide(winner);
                playingPlayers[winner].scoreWin(winner);
                playingPlayers[loser].scoreLoss(loser);
            }
            log(log, "Game took %s.%n", getElapsedTime(beforeGame, afterGame));

            //  Dump statistics.
            for (int j = 0; j < 2; j++) {
                log(log, "Player %d: %.1f, %s, %s%n", j, players[j].getScore(),
                        players[j].getScoreBreakdown(), players[j].getGitRevision());
            }
        }
        long afterMatch = System.currentTimeMillis();

        // Shut down.
        for (int i = 0; i < 2; i++) {
            players[i].quit();
        }
        log(log, "Match of %d games took %s.%n",
                GAME_COUNT, getElapsedTime(beforeMatch, afterMatch));

        log.close();
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
     * Print both to stderr and to our log file.
     */
    private static void log(PrintStream log, String format, Object ... args) {
        System.err.printf(format, (Object[]) args);
        log.printf(format, (Object[]) args);
    }

    /**
     * Returns the winning side, or Side.DRAW if it's a draw.
     */
    private int playGame(Player[] players, PrintStream log, int round) {
        Board board = new Board();
        Game game = new Game(board);

        // Set up.
        board.initializeTraditionalChess();
        for (int i = 0; i < 2; i++) {
            players[i].newGame();
        }

        // Play game.
        int sideToPlay = 0;
        int winner;
        while (true) {
            // Generate all legal moves for this side.
            List<Move> moveList = board.generateAllLegalMoves(sideToPlay);

            // If we have no legal moves left, then it's either stalemate or checkmate.
            if (moveList.isEmpty()) {
                if (board.getCheckIndex(sideToPlay) != -1) {
                    // Checkmate.
                    winner = Side.getOtherSide(sideToPlay);
                    break;
                } else {
                    // Stalemate.
                    winner = Side.DRAW;
                    break;
                }
            }

            // See if it's a draw.
            if (game.isDrawFrom50MoveRule()) {
                winner = Side.DRAW;
                break;
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

        game.writePgn(log, winner, round, players[0].getGitRevision(), players[1].getGitRevision());
        log.flush();

        return winner;
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

        new Match().start(args);
    }

    // --------------------------------------------------------------------------------

    private static class Player {
        private static final File GIT_ROOT = new File("/Users/lk/teamten");
        private final String mGitRevision;
        private final File mDir;
        private Process mProcess;
        private PrintWriter mWriter;
        private BufferedReader mReader;
        private int[] mDrawCount = new int[2];
        private int[] mWinCount = new int[2];
        private int[] mLossCount = new int[2];

        public Player(File tmpDir, String gitRevision) {
            mGitRevision = gitRevision;

            File rootDir;

            if (gitRevision.equals("current")) {
                rootDir = GIT_ROOT;
            } else {
                rootDir = new File(tmpDir, gitRevision);
                runProgram(tmpDir, "git", "clone", GIT_ROOT.toString(), gitRevision);
                runProgram(rootDir, "git", "checkout", gitRevision);
                runProgram(new File(rootDir, "java"), "ant");
            }

            mDir = new File(rootDir, "java/test");
        }

        public String getGitRevision() {
            return mGitRevision;
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
            return String.format("Wins: %d (%d,%d), Losses: %d (%d,%d), Draws: %d (%d,%d)",
                    mWinCount[Side.WHITE] + mWinCount[Side.BLACK],
                    mWinCount[Side.WHITE],
                    mWinCount[Side.BLACK],
                    mLossCount[Side.WHITE] + mLossCount[Side.BLACK],
                    mLossCount[Side.WHITE],
                    mLossCount[Side.BLACK],
                    mDrawCount[Side.WHITE] + mDrawCount[Side.BLACK],
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
            mWriter.println("go movetime " + MOVE_TIME);

            // Wait for move.
            while (true) {
                String line = readLine();
                if (line == null) {
                    return null;
                }

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

        private static void runProgram(File dir, String ... args) {
            String line = StringUtils.join(args, ' ');

            try {
                Process process = new ProcessBuilder((String[]) args).directory(dir).start();

                int status = process.waitFor();
                if (status != 0) {
                    throw new IllegalStateException("Got exit code " + status + " running " + line);
                }
            } catch (IOException e) {
                throw new IllegalStateException("Got exception running " + line + " (" + e + ")");
            } catch (InterruptedException e) {
                // Shutting down program, probably.
                return;
            }
        }
    }
}

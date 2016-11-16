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

// Copyright 2015 Lawrence Kesteloot

package com.teamten.chess;

import java.util.Date;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.FileOutputStream;

/**
 * Parses the Universal Chess Interface (UCI) protocol.
 *
 * http://wbec-ridderkerk.nl/html/UCIProtocol.html
 */
public class UciParser {
    private final BufferedReader mIn;
    private final PrintWriter mOut;
    private final PrintWriter mLog;
    private Board mBoard;
    private Game mGame;

    public UciParser(InputStream in, PrintStream out) throws IOException {
        mIn = new BufferedReader(new InputStreamReader(in));
        mOut = new PrintWriter(out, true);
        mLog = new PrintWriter(new FileOutputStream("uci.log"), true);
    }

    /**
     * Write a log line to the log file. Adds the date and newline.
     */
    private void log(String format, String ... args) {
        mLog.print(new Date());
        mLog.print(" - ");
        mLog.printf(format, (Object[]) args);
        mLog.println();
    }

    /**
     * Parse and execute a UCI line.
     */
    public boolean parseLine(String line) throws IOException {
        // Trim leading and trailing whitespace.
        line = line.trim();

        log("Got line: %s", line);

        // Parse into fields.
        String[] fields = line.split(" ");
        String command = fields[0];

        if (command.equals("uci")) {
            mOut.println("id name LKChess 1.0");
            mOut.println("id author Lawrence Kesteloot");
            // No options.
            mOut.println("uciok");
        } else if (command.equals("isready")) {
            mOut.println("readyok");
        } else if (command.equals("ucinewgame")) {
            mBoard = new Board();
            mGame = new Game(mBoard);
            mBoard.initializeTraditionalChess();
        } else if (command.equals("position")) {
            if (fields.length <= 2) {
                log("Not enough params to position command");
            } else {
                if (fields[1].equals("startpos")) {
                    mBoard.initializeTraditionalChess();
                } else {
                    log("Can't yet parse FEN string in position");
                    // mBoard.initializeWithFen(...);
                }
                if (fields.length > 2 && fields[2].equals("moves")) {
                    for (int i = 3; i < fields.length; i++) {
                        String lan = fields[i];

                        Move move = Move.parseLongAlgebraicNotation(mBoard, lan);
                        mGame.addMove(move);
                    }
                }
            }
        } else if (command.equals("go")) {
            long moveTime = 5000; // XXX

            // Parse sub-commands.
            int i = 1;
            while (i < fields.length) {
                if (fields[i].equals("movetime")) {
                    i++;
                    moveTime = Long.parseLong(fields[i]);
                } else {
                    System.err.println("Unknown sub-command of go: " + fields[i]);
                }
                i++;
            }

            ComputerPlayer player = new ComputerPlayer(mBoard, mGame, mBoard.getSide());
            ComputerPlayer.Result result = player.makeMove(moveTime);
            ComputerPlayer.EvaluatedMove evaluatedMove = result.mEvaluatedMove;
            Move move = evaluatedMove.getMove();
            if (move == null) {
                // Checkmate or stalemate.
            }
            mOut.printf("bestmove %s%n", move.getLongAlgebraicNotation());
        } else if (command.equals("quit")) {
            return false;
        } else {
            log("Unknown command: %s", command);
        }

        return true;
    }

    /**
     * Loop, reading a line and executing it, until done.
     */
    public void start() throws IOException {
        boolean keepGoing;

        do {
            String line = mIn.readLine();
            if (line == null) {
                // End of file.
                return;
            }

            keepGoing = parseLine(line);
        } while (keepGoing);
    }

    public static void main(String[] args) throws IOException {
        UciParser uciParser = new UciParser(System.in, System.out);

        uciParser.start();
    }
}

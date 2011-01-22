// Copyright 2011 Lawrence Kesteloot

package com.teamten.chess.server;

import org.apache.commons.io.IOUtils;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.teamten.chess.Board;
import com.teamten.chess.Game;
import com.teamten.chess.IllegalMoveException;
import com.teamten.chess.Piece;
import com.teamten.chess.Side;
import com.teamten.chess.Move;
import com.teamten.chess.ComputerPlayer;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Servlet to serve a chess game.
 */
public class ChessServlet extends HttpServlet {
    private static final int GAME_COLUMN_MOVES = 70;

    @Override // HttpServlet
    public void doGet(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        String filename = request.getPathInfo();
        if (filename.equals("/")) {
            showBoard(request, response);
        } else if (filename.equals("/menu")) {
            showMenu(request, response);
        } else if (filename.endsWith(".css")) {
            serveStaticFile(response, "text/css", filename.substring(1));
        } else if (filename.endsWith(".png")) {
            serveStaticFile(response, "image/png", filename.substring(1));
        } else if (filename.endsWith(".js")) {
            serveStaticFile(response, "text/javascript", filename.substring(1));
        }
    }

    @Override // HttpServlet
    public void doPost(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Game game;
        Board board;

        String gameParameter = request.getParameter("game");
        if (gameParameter == null) {
            board = new Board();
            game = new Game(board);
        } else {
            game = Game.deserialize(gameParameter);
            board = game.getBoard();
        }
        String message = null;
        String highlightSquare = null;
        boolean playMove = false;
        boolean autoPlay = "on".equals(request.getParameter("autoPlay"));

        // Figure out what the user wants us to do.
        String command = request.getParameter("command");
        if (command != null) {
            if (command.equals("computerMove")) {
                // Have computer make a move.
                ComputerPlayer player = new ComputerPlayer(board, game, board.getSide());
                ComputerPlayer.EvaluatedMove evaluatedMove = player.makeMove();
                if (evaluatedMove.getMove() == null) {
                    message = "Cannot move";
                } else {
                    StringBuilder builder = new StringBuilder();
                    int moveNumber = (game.getMoveCount() + 1)/2;
                    builder.append("Principal variation: ");
                    if (game.getMoveCount() % 2 == 0) {
                        builder.append(moveNumber);
                        builder.append(". ... ");
                    }
                    for (ComputerPlayer.EvaluatedMove e = evaluatedMove;
                            e != null && e.getMove() != null;
                            e = e.getNextMove()) {

                        Move move = e.getMove();
                        if (move.getMovingPiece().getSide() == Side.WHITE) {
                            builder.append(moveNumber);
                            builder.append(". ");
                            builder.append(move.getFigurineAlgebraicNotation());
                            builder.append(" ");
                        } else {
                            moveNumber++;
                            builder.append(move.getFigurineAlgebraicNotation());
                            builder.append("&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;");
                        }
                    }

                    builder.append(String.format("(%.1f)", evaluatedMove.getScore()));

                    message = builder.toString();
                }
            } else if (command.equals("undo")) {
                if (game.getMoveCount() > 0) {
                    game.undoMove();
                }
            } else if (command.equals("redo")) {
                game.redoMove();
            } else if (command.equals("newGame")) {
                board = new Board();
                board.initializeTraditionalChess();
                game = new Game(board);
            } else if (command.equals("new960Game")) {
                board = new Board();
                board.initializeChess960();
                game = new Game(board);
            } else if (command.equals("submitMove")) {
                String moveString = request.getParameter("move");
                try {
                    Move move = board.parseMove(moveString);
                    game.addMove(move);

                    // Hand-entered move was okay. See if we want to automatically
                    // play for the other side.
                    playMove = autoPlay;
                } catch (IllegalMoveException e) {
                    System.out.println(e.getMessage());
                    message = e.getMessage();
                    int checkIndex = e.getCheckIndex();
                    if (checkIndex != -1) {
                        highlightSquare = Board.getPosition(checkIndex);
                    }
                } catch (IllegalArgumentException e) {
                    System.out.println(e.getMessage());
                    message = e.getMessage();
                }
            }
        }

        // See if we're in checkmate or stalemate.
        List<Move> moveList = board.generateAllMoves(board.getSide(), false);
        board.removeIllegalMoves(moveList);
        String mateMessage = null;
        if (moveList.isEmpty()) {
            int checkIndex = board.getCheckIndex(board.getSide());
            if (checkIndex != -1) {
                highlightSquare = Board.getPosition(checkIndex);
                mateMessage = "Checkmate from " + highlightSquare;
            } else {
                mateMessage = "Stalemate";
            }
        }
        if (mateMessage != null) {
            if (message == null) {
                message = mateMessage;
            } else {
                message = message + " (" + mateMessage + ")";
            }
        }

        String x = game.serialize();
        String y = Game.deserialize(x).serialize();
        if (!x.equals(y)) {
            // Sanity check.
            System.out.println("***********************************");
            System.out.println(x);
            System.out.println(y);
            System.out.println("***********************************");
        }

        String url = "/chess/?game=" + game.serialize();
        if (message != null) {
            url += "&message=" + URLEncoder.encode(message, "ASCII");
        }
        if (playMove) {
            url += "&playMove=on";
        }
        if (autoPlay) {
            url += "&setAutoPlay=on";
        }
        if (highlightSquare != null) {
            url += "&highlightSquare=" + highlightSquare;
        }
        System.out.println("Redirecting to " + url);

        // Reload with GET.
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", url);
    }

    /**
     * Poor man's static file server.
     */
    private static void serveStaticFile(HttpServletResponse response,
            String contentType, String filename) throws IOException {

        response.setContentType(contentType);
        response.setStatus(HttpServletResponse.SC_OK);

        IOUtils.copy(
                ChessServlet.class.getResourceAsStream(filename),
                response.getOutputStream());
    }

    /**
     * Dump the chess board in HTML.
     */
    private void showBoard(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        Board board = null;
        Game game = null;
        String message = null;
        boolean playMove = false;
        boolean setAutoPlay = false;
        String highlightSquare = null;
        boolean iPad = false;

        // Parse the headers.
        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null) {
            if (userAgent.contains("iPad")) {
                iPad = true;
            }
        }

        // Parse the request string.
        String query = request.getQueryString();
        System.out.println("GET: " + query);
        if (query != null) {
            String[] fields = query.split("&");
            for (String field : fields) {
                String[] subFields = field.split("=", 2);
                if (subFields.length != 2) {
                    System.out.println("Field \"" + field + "\" doesn't split.");
                    continue;
                }
                String key = subFields[0];
                String value = subFields[1];

                if (key.equals("game")) {
                    game = Game.deserialize(value);
                    board = game.getBoard();
                } else if (key.equals("message")) {
                    try {
                        message = URLDecoder.decode(value, "ASCII");
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                } else if (key.equals("playMove")) {
                    playMove = true;
                } else if (key.equals("setAutoPlay")) {
                    setAutoPlay = true;
                } else if (key.equals("highlightSquare")) {
                    highlightSquare = value;
                } else if (key.equals("ipad")) {
                    iPad = true;
                }
            }
        }

        // New query.
        if (board == null) {
            board = new Board();
            board.initializeTraditionalChess();
            game = new Game(board);
        }

        // Set headers.
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter writer = response.getWriter();

        writer.println("<html><head>");
        writer.println("<title>Chess</title>");
        writer.println("<link type='text/css' rel='stylesheet' href='main.css'>");
        writer.println("<script>");
        writer.println("var g_playMove = " + playMove + ";");
        generateLegalMoves(writer, board);
        generateAtRisk(writer, board);
        writer.println("</script>");
        writer.println("<script src='jquery-1.4.2.min.js'></script>");
        writer.println("<script src='chess.js'></script>");
        writer.println("</head><body class='board" + (iPad ? " ipad" : "") + "'>");

        writer.println("<table cellpadding='0' cellspacing='0' class='page'><tr><td valign='top'>");

        writer.println("<table cellpadding='0' cellspacing='0' class='board'>");
        for (int rank = Board.SIZE; rank >= 1; rank--) {
            writer.println("<tr>");
            for (int file = 1; file <= Board.SIZE; file++) {
                int index = Board.getIndex(file, rank);
                String position = Board.getPosition(index);

                String color = ((file + rank) % 2 == 0) ? "black" : "white";
                writer.print("<td class='square " + color
                        + (position.equals(highlightSquare) ? " highlight" : "")
                        + "' id='"
                        + position + "'><div>");
                Piece piece = board.getPiece(index);
                if (piece == Piece.EMPTY) {
                    writer.print("&nbsp;");
                } else {
                    writer.print(piece.getHtmlCharacter());
                }
                writer.println("</div></td>");
            }
            if ((rank == 1 && board.getSide() == Side.WHITE)
                    || (rank == Board.SIZE && board.getSide() == Side.BLACK)) {

                writer.println("<td class='side'><div>&middot;</div></td>");
            }
            writer.println("</tr>");
        }
        writer.println("</table>"); // Board
        writer.println("<form method='POST' id='form'><p>");
        writer.println("<input type='hidden' name='game' value='" + game.serialize() + "'>");
        writer.println("<input type='hidden' id='command' name='command'>");
        if (!iPad) {
            writer.println("<input type='button' id='makeMoveButton' value='Make Move' id='makeMoveButton'>");
        }
        writer.println("<input type='button' id='undoButton' value='Undo'>");
        writer.println("<input type='button' id='redoButton' value='Redo'>");
        if (!iPad) {
            writer.println("<input type='button' id='newGameButton' value='New Game'>");
            writer.println("<input type='button' id='new960GameButton' value='New 960 Game'>");
        }
        writer.println("<a href='/chess/menu'>Menu</a>");
        writer.println("</p><p>");
        writer.println("<input type='text' id='moveTextField' name='move'>");
        writer.println("<input type='checkbox' id='autoPlayCheckbox' name='autoPlay'"
                + (setAutoPlay ? " checked" : "")
                + "> <label id='autoPlayLabel' for='autoPlayCheckbox'>Auto play</label> ");
        writer.println("<input type='checkbox' id='showAtRiskCheckbox'"
                + "> <label id='showAtRiskLabel' for='showAtRiskCheckbox'>Show At-Risk</label> ");
        writer.println("</p></form>");
        if (message != null) {
            writer.println("<div class='message'>" + message + "</div>");
        }

        if (!iPad) {
            int numColumns = (game.getMoveCount() + GAME_COLUMN_MOVES - 1)/GAME_COLUMN_MOVES;
            for (int column = 0; column < numColumns; column++) {
                writer.println("</td><td valign='top'>"); // Vertical split.

                writer.println("<table cellpadding='0' cellspacing='0' class='game'>"); // Game
                for (int i = column*GAME_COLUMN_MOVES; i < game.getMoveCount()
                        && i < (column + 1)*GAME_COLUMN_MOVES; i++) {

                    Move move = game.getMove(i);
                    if (i % 2 == 0) {
                        writer.printf("<tr><td class='number'>%d.</td><td class='white'>%s</td>%n",
                                i/2 + 1, move.getFigurineAlgebraicNotation());
                    } else {
                        writer.printf("<td class='black'>%s</td></tr>%n",
                                move.getFigurineAlgebraicNotation());
                    }
                        }
                writer.println("</table>"); // Game
            }
        }

        writer.println("</td></tr></table>"); // Page

        writer.println("</body></html>");
    }

    /**
     * Generate JavaScript for all the legal moves the user can do right now.
     */
    private void generateLegalMoves(PrintWriter writer, Board board) {
        writer.println("var g_legalMoves = {");
        for (int index = 0; index < Board.NUM_SQUARES; index++) {
            Piece piece = board.getPiece(index);
            if (piece != Piece.EMPTY && piece.getSide() == board.getSide()) {
                String position = Board.getPosition(index);
                writer.print("    \"" + position + "\": [ ");
                List<Move> moveList = new ArrayList<Move>();
                piece.addMoves(board, index, false, moveList);
                board.removeIllegalMoves(moveList);
                boolean firstMove = true;
                for (Move move : moveList) {
                    if (firstMove) {
                        firstMove = false;
                    } else {
                        writer.print(", ");
                    }

                    writer.print("\"" + Board.getPosition(move.getToIndex()) + "\"");
                }
                writer.println(" ],");
            }
        }
        writer.println("};");
    }

    /**
     * Generate JavaScript for all the pieces that are at risk right now.
     */
    private void generateAtRisk(PrintWriter writer, Board board) {
        // Map from position to count.
        Map<String,int[]> positionCountMap = new HashMap<String,int[]>();

        // Figure out the squares at risk (attacked by other side).
        List<Move> moveList = board.generateAllMoves(Side.getOtherSide(board.getSide()), true);
        board.removeIllegalMoves(moveList);
        for (Move move : moveList) {
            int toIndex = move.getToIndex();
            String toPosition = Board.getPosition(toIndex);

            int[] count = positionCountMap.get(toPosition);
            if (count == null) {
                count = new int[1];
                positionCountMap.put(toPosition, count);
            }

            count[0]++;
        }

        writer.println("var g_atRisk = {");
        for (Map.Entry<String,int[]> entry : positionCountMap.entrySet()) {
            writer.println("    \"" + entry.getKey() + "\": " + entry.getValue()[0] + ",");
        }

        writer.println("};");
    }

    /**
     * Top-level menu.
     */
    private void showMenu(HttpServletRequest request,
            HttpServletResponse response) throws IOException {

        // Set headers.
        response.setContentType("text/html; charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);

        PrintWriter writer = response.getWriter();
        writer.println("<html><head>");
        writer.println("<title>Chess</title>");
        writer.println("<link type='text/css' rel='stylesheet' href='main.css'>");
        writer.println("<script src='jquery-1.4.2.min.js'></script>");
        writer.println("<script src='menu.js'></script>");
        writer.println("</head><body class='menu'>");
        writer.println("<h1>Chess</h1>");
        writer.println("<button id='onePlayerButton'>1 Player</button>");
        writer.println("<button id='twoPlayerButton'>2 Player</button>");
        writer.println("<form method='POST' id='form' action='/chess/'>");
        writer.println("<input type='hidden' id='command' name='command'>");
        writer.println("<input type='hidden' id='autoPlayCheckbox' name='autoPlay'>");
        writer.println("</form>");
        writer.println("</body></html>");
    }
}

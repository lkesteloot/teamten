package com.teamten.tex;

import com.teamten.util.Files;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Returns TeX-like tokens from an input stream.
 */
public class TexTokenizer {
    private final Reader mReader;
    private int mCh = -1;
    private State mState;
    private final StringBuilder mCommand = new StringBuilder();

    private enum State {
        NORMAL,
        SKIP_WHITESPACE,
        START_OF_COMMAND,
        REST_OF_COMMAND,
        SKIP_COMMENT
    }

    public TexTokenizer(InputStream inputStream) throws IOException {
        mReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
        mState = State.NORMAL;

        // Prime the pump.
        mCh = Files.nextCodePoint(mReader);
    }

    /**
     * Return the next token in the stream, or null on end of file.
     */
    public Token next() throws IOException {
        // Get characters until we have a valid token.
        while (true) {
            if (mCh == -1) {
                // End of file.
                return null;
            }

            switch (mState) {
                case NORMAL:
                    if (mCh == '\\') {
                        mState = State.START_OF_COMMAND;
                        mCh = Files.nextCodePoint(mReader);
                    } else if (mCh == '%') {
                        mState = State.SKIP_COMMENT;
                        mCh = Files.nextCodePoint(mReader);
                    } else if (mCh == '\n' || mCh == ' ') {
                        // Translate to space.
                        Token token = new TextToken(" ");
                        mCh = Files.nextCodePoint(mReader);
                        mState = State.SKIP_WHITESPACE;
                        return token;
                    } else {
                        Token token = new TextToken(codePointToString(mCh));
                        mCh = Files.nextCodePoint(mReader);
                        return token;
                    }
                    break;

                case SKIP_WHITESPACE:
                    if (Character.isWhitespace(mCh)) {
                        mCh = Files.nextCodePoint(mReader);
                    } else {
                        mState = State.NORMAL;
                    }
                    break;

                case START_OF_COMMAND:
                    // The first character of a command.
                    if (Character.isLetter(mCh)) {
                        mCommand.setLength(0);
                        mCommand.appendCodePoint(mCh);
                        mCh = Files.nextCodePoint(mReader);
                        mState = State.REST_OF_COMMAND;
                    } else {
                        Token token = new CommandToken(codePointToString(mCh));
                        mCh = Files.nextCodePoint(mReader);
                        mState = State.NORMAL;
                        return token;
                    }
                    break;

                case REST_OF_COMMAND:
                    if (Character.isLetter(mCh)) {
                        mCommand.appendCodePoint(mCh);
                        mCh = Files.nextCodePoint(mReader);
                    } else {
                        Token token = new CommandToken(mCommand.toString());
                        mState = State.SKIP_WHITESPACE;
                        return token;
                    }
                    break;

                case SKIP_COMMENT:
                    if (mCh == '\n') {
                        mState = State.NORMAL;
                    }
                    mCh = Files.nextCodePoint(mReader);
                    break;
            }
        }
    }

    /**
     * Simple routine to convert a single code point to a string, because surprisingly this isn't a thing in the standard library.
     */
    private static String codePointToString(int codePoint) {
        int[] codePoints = new int[1];
        codePoints[0] = codePoint;
        return new String(codePoints, 0, 1);
    }
}

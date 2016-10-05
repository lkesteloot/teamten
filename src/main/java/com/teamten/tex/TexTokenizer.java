package com.teamten.tex;

import com.teamten.util.CodePoints;

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
        mCh = CodePoints.nextCodePoint(mReader);
    }

    /**
     * Return the next token in the stream, or -1 on end of file.
     */
    public int next() throws IOException {
        // Get characters until we have a valid token.
        while (true) {
            if (mCh == -1) {
                // End of file.
                return -1;
            }

            switch (mState) {
                case NORMAL:
                    if (mCh == '\\') {
                        mState = State.START_OF_COMMAND;
                        mCh = CodePoints.nextCodePoint(mReader);
                    } else if (mCh == '%') {
                        mState = State.SKIP_COMMENT;
                        mCh = CodePoints.nextCodePoint(mReader);
                    } else if (mCh == '\n' || mCh == ' ') {
                        // Translate to space.
                        mCh = CodePoints.nextCodePoint(mReader);
                        mState = State.SKIP_WHITESPACE;
                        return ' ';
                    } else {
                        int ch = mCh;
                        mCh = CodePoints.nextCodePoint(mReader);
                        return ch;
                    }
                    break;

                case SKIP_WHITESPACE:
                    if (Character.isWhitespace(mCh)) {
                        mCh = CodePoints.nextCodePoint(mReader);
                    } else {
                        mState = State.NORMAL;
                    }
                    break;

                case START_OF_COMMAND:
                    // The first character of a command.
                    if (Character.isLetter(mCh)) {
                        mCommand.setLength(0);
                        mCommand.appendCodePoint(mCh);
                        mCh = CodePoints.nextCodePoint(mReader);
                        mState = State.REST_OF_COMMAND;
                    } else {
                        int token = Token.fromCharacter(mCh);
                        mCh = CodePoints.nextCodePoint(mReader);
                        mState = State.NORMAL;
                        return token;
                    }
                    break;

                case REST_OF_COMMAND:
                    if (Character.isLetter(mCh)) {
                        mCommand.appendCodePoint(mCh);
                        mCh = CodePoints.nextCodePoint(mReader);
                    } else {
                        int token = Token.fromKeyword(mCommand.toString());
                        if (token == -1) {
                            throw new IllegalStateException("unknown token: " + mCommand);
                        }
                        mState = State.SKIP_WHITESPACE;
                        return token;
                    }
                    break;

                case SKIP_COMMENT:
                    if (mCh == '\n') {
                        mState = State.NORMAL;
                    }
                    mCh = CodePoints.nextCodePoint(mReader);
                    break;
            }
        }
    }
}

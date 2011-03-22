// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

/**
 * Processes a single Jawa file into a Java file, expanding any
 * included templates.
 */
public class JawaProcessor {
    /**
     * The character being read is at this column, 1-based.
     */
    private PrintStream mWriter;
    private static enum State {
        // Parsing Java code.
        NORMAL,
        // Capturing the indent at the beginning of the line.
        INDENT,
        // Parsing the filename of an included template.
        TEMPLATE_FILENAME,
    }
    private State mState;
    private String mIncludedFilename;
    private String mIndent;

    public void processFile(String inputFilename, String outputFilename)
        throws IOException {

        Reader reader = new FileReader(inputFilename);
        resetState();

        try {
            mWriter = new PrintStream(outputFilename);

            try {
                int ch;
                while ((ch = reader.read()) != -1) {
                    processChar((char) ch);
                }
            } finally {
                mWriter.close();
            }
        } finally {
            reader.close();
        }
    }

    private void resetState() {
        mState = State.INDENT;
        mIncludedFilename = "";
        mIndent = "";
    }

    private void processChar(char ch) throws IOException {
        switch (mState) {
            case NORMAL:
                if (ch == '`') {
                    mState = State.TEMPLATE_FILENAME;
                } else if (ch == '\n') {
                    mState = State.INDENT;
                    mIndent = "";
                    mWriter.print(ch);
                } else {
                    mWriter.print(ch);
                }
                break;

            case TEMPLATE_FILENAME:
                if (ch == '`') {
                    new TemplateProcessor()
                        .withPrintStream(mWriter)
                        .withIndent(mIndent)
                        .processFile(mIncludedFilename);
                    mState = State.NORMAL;
                    mIncludedFilename = "";
                } else if (ch == '\n') {
                    System.err.println("Unterminated include");
                    mState = State.INDENT;
                    mIndent = "";
                } else {
                    mIncludedFilename += ch;
                }
                break;

            case INDENT:
                if (ch == ' ' || ch == '\t') {
                    mIndent += ch;
                    mWriter.print(ch);
                } else if (ch == '`') {
                    mState = State.TEMPLATE_FILENAME;
                } else if (ch == '\n') {
                    mIndent = "";
                    mWriter.print(ch);
                } else {
                    mState = State.NORMAL;
                    mWriter.print(ch);
                }
                break;
        }
    }
}

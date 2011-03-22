// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;

/**
 * Processes a template into Java code.
 */
public class TemplateProcessor {
    /**
     * Generates prettier but perhaps less performant code.
     */
    private static final boolean PRETTY_CODE = true;
    private static enum State {
        // Processing template text.
        NORMAL,
        // Was NORMAL, just saw open brace.
        SAW_OPEN_BRACE,
        // Saw second open brace, now in expression.
        EXPRESSION,
        // In expression, saw close brace.
        SAW_CLOSE_BRACE,
    }
    private State mState = State.NORMAL;
    private PrintStream mWriter;

    public void processFile(String filename, PrintStream writer)
        throws IOException {

        System.out.println("Including <" + filename + ">");

        mWriter = writer;
        Reader inputReader = new FileReader(filename);

        try {
            startNormal();

            int ch;
            while ((ch = inputReader.read()) != -1) {
                processChar((char) ch);
            }

            endNormal();
        } finally {
            inputReader.close();
        }
    }

    private void processChar(char ch) {
        switch (mState) {
            case NORMAL:
                if (ch == '{') {
                    mState = State.SAW_OPEN_BRACE;
                } else if (ch == '\n') {
                    if (PRETTY_CODE) {
                        mWriter.print("\\n\"\n+ \"");
                    } else {
                        mWriter.print("\\n");
                    }
                } else if (ch == '"') {
                    mWriter.print("\"");
                } else {
                    mWriter.print(ch);
                }
                break;

            case SAW_OPEN_BRACE:
                if (ch == '{') {
                    endNormal();
                    startExpression();
                    mState = State.EXPRESSION;
                } else {
                    mWriter.print('{');
                    mWriter.print(ch);
                    mState = State.NORMAL;
                }
                break;

            case EXPRESSION:
                if (ch == '}') {
                    mState = State.SAW_CLOSE_BRACE;
                } else {
                    mWriter.print(ch);
                }
                break;

            case SAW_CLOSE_BRACE:
                if (ch == '}') {
                    endExpression();
                    startNormal();
                    mState = State.NORMAL;
                } else {
                    mWriter.print('}');
                    mWriter.print(ch);
                    mState = State.EXPRESSION;
                }
                break;
        }
    }

    private void startNormal() {
        mWriter.print("writer.print(\"");
    }

    private void endNormal() {
        mWriter.println("\");");
    }

    private void startExpression() {
        mWriter.print("writer.print(");
    }

    private void endExpression() {
        mWriter.println(");");
    }
}

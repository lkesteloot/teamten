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
    /**
     * The indent equivalent to the print statement, minus the plus sign.
     */
    private static enum State {
        // Processing template text.
        NORMAL,
        // Was NORMAL, just saw open brace.
        SAW_OPEN_BRACE,
        // Saw second open brace, now in expression.
        EXPRESSION,
        // In expression, saw close brace.
        SAW_CLOSE_BRACE,
        // Saw % after open brace, now in statement.
        STATEMENT,
        // In statement, saw percent.
        SAW_PERCENT,
    }
    private State mState = State.NORMAL;
    private PrintStream mWriter;
    private String mIndent = "";
    private int mCounter;
    private String mWriterName;
    private String mPrintIndent;

    public TemplateProcessor() {
        setCounter(0);
    }

    public TemplateProcessor withPrintStream(PrintStream writer) {
        mWriter = writer;
        return this;
    }

    public TemplateProcessor withIndent(String indent) {
        mIndent = indent;
        return this;
    }

    public TemplateProcessor withCounter(int counter) {
        setCounter(counter);
        return this;
    }

    /**
     * Can probably remove this now that we put the code inside its own block.
     */
    private void setCounter(int counter) {
        mCounter = counter;

        mWriterName = "jawaWriter";
        if (mCounter > 0) {
            mWriterName += mCounter;
        }

        // Compute print indent string.
        int length = mWriterName.length() + 5;
        mPrintIndent = "";
        for (int i = 0; i < length; i++) {
            mPrintIndent += " ";
        }
    }

    public void processFile(String filename)
        throws IOException {

        System.out.println("Including <" + filename + ">");

        Reader inputReader = new FileReader(filename);

        mWriter.print("{\n" + mIndent);
        mWriter.print("java.io.PrintStream " + mWriterName
                + " = com.teamten.jawa.Jawa.getPrintStream();\n" + mIndent);

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

        mWriter.print("}\n" + mIndent);
    }

    private void processChar(char ch) {
        switch (mState) {
            case NORMAL:
                if (ch == '{') {
                    mState = State.SAW_OPEN_BRACE;
                } else if (ch == '\n') {
                    if (PRETTY_CODE) {
                        mWriter.print("\\n\"\n" + mIndent
                                + mPrintIndent + "+ \"");
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
                } else if (ch == '%') {
                    endNormal();
                    startStatement();
                    mState = State.STATEMENT;
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

            case STATEMENT:
                if (ch == '%') {
                    mState = State.SAW_PERCENT;
                } else {
                    mWriter.print(ch);
                }
                break;

            case SAW_PERCENT:
                if (ch == '}') {
                    endStatement();
                    startNormal();
                    mState = State.NORMAL;
                } else {
                    mWriter.print('%');

                    if (ch == '%') {
                        // Nothing, stay in this state.
                    } else {
                        mWriter.print(ch);
                        mState = State.STATEMENT;
                    }
                }
                break;
        }
    }

    private void startNormal() {
        mWriter.print(mWriterName + ".print(\"");
    }

    private void endNormal() {
        mWriter.print("\");\n" + mIndent);
    }

    private void startExpression() {
        mWriter.print(mWriterName + ".print(");
    }

    private void endExpression() {
        mWriter.print(");\n" + mIndent);
    }

    private void startStatement() {
        // Nothing.
    }

    private void endStatement() {
        // Nothing.
    }
}

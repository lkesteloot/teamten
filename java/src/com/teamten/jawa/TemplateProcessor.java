// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.FileReader;
import java.io.IOException;
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
     * How to output the template.
     */
    private static enum OutputMethod {
        // PrintStream instance.
        PRINT_STREAM,
        // StringBuilder instance.
        STRING_BUILDER,
    }
    private static OutputMethod OUTPUT_METHOD = OutputMethod.STRING_BUILDER;
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
    private StringBuilder mWriter;
    private String mIndent = "";
    private int mCounter;
    private String mWriterName;
    private String mAppendMethod;
    private String mPrintIndent;

    public TemplateProcessor() {
        setCounter(0);
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

        switch (OUTPUT_METHOD) {
            case PRINT_STREAM:
                mAppendMethod = mWriterName + ".print";
                break;

            case STRING_BUILDER:
                mAppendMethod = mWriterName + ".append";
                break;
        }

        // Compute print indent string.
        int length = mAppendMethod.length() - 1;
        mPrintIndent = "";
        for (int i = 0; i < length; i++) {
            mPrintIndent += " ";
        }
    }

    public String processFile(String filename)
        throws IOException {

        System.out.println("Including <" + filename + ">");

        Reader inputReader = new FileReader(filename);
        mWriter = new StringBuilder();

        switch (OUTPUT_METHOD) {
            case PRINT_STREAM:
                mWriter.append("{\n" + mIndent);
                mWriter.append("java.io.PrintStream " + mWriterName
                        + " = com.teamten.jawa.Jawa.getPrintStream();\n"
                        + mIndent);
                break;

            case STRING_BUILDER:
                mWriter.append("StringBuilder " + mWriterName
                        + " = new StringBuilder();\n"
                        + mIndent);
                break;
        }

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

        switch (OUTPUT_METHOD) {
            case PRINT_STREAM:
                mWriter.append("}\n" + mIndent);
                break;

            case STRING_BUILDER:
                mWriter.append("return " + mWriterName + ".toString();\n"
                        + mIndent);
                break;
        }

        return mWriter.toString();
    }

    private void processChar(char ch) {
        switch (mState) {
            case NORMAL:
                if (ch == '{') {
                    mState = State.SAW_OPEN_BRACE;
                } else if (ch == '\n') {
                    if (PRETTY_CODE) {
                        mWriter.append("\\n\"\n" + mIndent
                                + mPrintIndent + "+ \"");
                    } else {
                        mWriter.append("\\n");
                    }
                } else if (ch == '\\') {
                    mWriter.append("\\\\");
                } else if (ch == '"') {
                    mWriter.append("\\\"");
                } else {
                    mWriter.append(ch);
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
                    mWriter.append('{');
                    mWriter.append(ch);
                    mState = State.NORMAL;
                }
                break;

            case EXPRESSION:
                if (ch == '}') {
                    mState = State.SAW_CLOSE_BRACE;
                } else {
                    mWriter.append(ch);
                }
                break;

            case SAW_CLOSE_BRACE:
                if (ch == '}') {
                    endExpression();
                    startNormal();
                    mState = State.NORMAL;
                } else {
                    mWriter.append('}');
                    mWriter.append(ch);
                    mState = State.EXPRESSION;
                }
                break;

            case STATEMENT:
                if (ch == '%') {
                    mState = State.SAW_PERCENT;
                } else {
                    mWriter.append(ch);
                }
                break;

            case SAW_PERCENT:
                if (ch == '}') {
                    endStatement();
                    startNormal();
                    mState = State.NORMAL;
                } else {
                    mWriter.append('%');

                    if (ch == '%') {
                        // Nothing, stay in this state.
                    } else {
                        mWriter.append(ch);
                        mState = State.STATEMENT;
                    }
                }
                break;
        }
    }

    private void startNormal() {
        mWriter.append(mAppendMethod + "(\"");
    }

    private void endNormal() {
        mWriter.append("\");\n" + mIndent);
    }

    private void startExpression() {
        mWriter.append(mAppendMethod + "(");
    }

    private void endExpression() {
        mWriter.append(");\n" + mIndent);
    }

    private void startStatement() {
        // Nothing.
    }

    private void endStatement() {
        // Nothing.
    }
}

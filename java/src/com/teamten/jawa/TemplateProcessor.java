// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

/**
 * Processes a template into Java code that prints the template contents.
 */
public class TemplateProcessor {
    /**
     * Name of the variable we're writing to.
     */
    private static final String WRITER_NAME = "jawaWriter";
    /**
     * How to output the template.
     */
    private static enum OutputMethod {
        // PrintStream instance.
        PRINT_STREAM,
        // StringBuilder instance.
        STRING_BUILDER,
    }
    private OutputMethod mOutputMethod;
    /**
     * Parser state machine.
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
    /**
     * Generating the Java code into this.
     */
    private StringBuilder mWriter;
    /**
     * Indent the entire Java code by this much.
     */
    private String mIndent = "";
    /**
     * The full method (including writer name) used to append template
     * text.
     */
    private String mAppendMethod;
    /**
     * An indent for lines that go below the writer line.
     */
    private String mAppendMethodIndent;
    /**
     * These four keep track of the text before and after a statement on
     * a line. If, at the end of the line, these are entirely blank, we
     * remove them.
     */
    private int mPreStatementBegin;
    private int mPreStatementEnd;
    private int mPostStatementBegin;
    private int mPostStatementEnd;

    public TemplateProcessor() {
        setOutputMethod(OutputMethod.STRING_BUILDER);
    }

    /**
     * Configures the indent at the front of every Java line.
     */
    public TemplateProcessor withIndent(String indent) {
        mIndent = indent;
        return this;
    }

    /**
     * Specifies how the run-time code should output the template.
     */
    private void setOutputMethod(OutputMethod outputMethod) {
        mOutputMethod = outputMethod;

        switch (mOutputMethod) {
            case PRINT_STREAM:
                mAppendMethod = WRITER_NAME + ".print";
                break;

            case STRING_BUILDER:
                mAppendMethod = WRITER_NAME + ".append";
                break;
        }

        // Compute print indent string.
        int length = mAppendMethod.length() - 1;
        mAppendMethodIndent = "";
        for (int i = 0; i < length; i++) {
            mAppendMethodIndent += " ";
        }
    }

    /**
     * Process the template in the file and return Java code that will
     * print it. The trailing newline from the template is removed.
     */
    public String processFile(String filename) throws IOException {
        System.out.println("Processing <" + filename + ">");

        // Read the template all at once so we can pre-process it a bit.
        String template = FileUtils.readFileToString(new File(filename));
        // Strip trailing \n.
        int length = template.length();
        if (length > 0 && template.charAt(length - 1) == '\n') {
            template = template.substring(0, length - 1);
        }

        // We'll write Java to this builder.
        mWriter = new StringBuilder();

        // Create our output variable.
        switch (mOutputMethod) {
            case PRINT_STREAM:
                mWriter.append("java.io.PrintStream " + WRITER_NAME
                        + " = com.teamten.jawa.Jawa.getPrintStream();\n"
                        + mIndent);
                break;

            case STRING_BUILDER:
                mWriter.append("StringBuilder " + WRITER_NAME
                        + " = new StringBuilder();\n"
                        + mIndent);
                break;
        }

        startNewLine();
        startNormal();

        for (int i = 0; i < template.length(); i++) {
            char ch = template.charAt(i);
            processChar(ch);
        }

        endNormal();

        switch (mOutputMethod) {
            case PRINT_STREAM:
                // Nothing.
                break;

            case STRING_BUILDER:
                mWriter.append("return " + WRITER_NAME + ".toString();\n"
                        + mIndent);
                break;
        }

        return mWriter.toString();
    }

    private void startNewLine() {
        mPreStatementBegin = mWriter.length();
        mPreStatementEnd = -1;
        mPostStatementBegin = -1;
        mPostStatementEnd = -1;
    }

    private void processChar(char ch) {
        switch (mState) {
            case NORMAL:
                if (ch == '{') {
                    mState = State.SAW_OPEN_BRACE;
                } else if (ch == '\n') {
                    mPostStatementEnd = mWriter.length();
                    boolean deleted = possiblyDeleteLine();
                    if (!deleted) {
                        mWriter.append("\\n");
                    }
                    mWriter.append("\"\n" + mIndent
                            + mAppendMethodIndent + "+ \"");
                    startNewLine();
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
                    if (mPreStatementEnd == -1) {
                        mPreStatementEnd = mWriter.length();
                    } else {
                        // Disable for this line.
                        mPreStatementBegin = -1;
                    }
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
                    if (mPreStatementEnd != -1) {
                        mPostStatementBegin = mWriter.length();
                    }
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

    private boolean possiblyDeleteLine() {
        if (mPreStatementBegin != -1
                && mPreStatementEnd != -1
                && mPostStatementBegin != -1
                && isBlank(mPreStatementBegin, mPreStatementEnd)
                && isBlank(mPostStatementBegin, mPostStatementEnd)) {

            if (mPreStatementBegin < mPreStatementEnd) {
                mWriter.delete(mPreStatementBegin, mPreStatementEnd);
            }
            if (mPostStatementBegin < mPostStatementEnd) {
                mWriter.delete(mPostStatementBegin, mPostStatementEnd);
            }

            return true;
        }

        return false;
    }

    private boolean isBlank(int begin, int end) {
        for (int i = begin; i < end; i++) {
            char ch = mWriter.charAt(i);
            if (ch != ' ' && ch != '\t') {
                return false;
            }
        }

        return true;
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

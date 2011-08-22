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
        // Saw second open brace, now in expression head.
        EXPRESSION_HEAD,
        // Past (optional) expression head.
        EXPRESSION,
        // The flag after the colon.
        EXPRESSION_FLAG,
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
    /**
     * The flag after the colon after the start of an expression, while it's being
     * parsed.
     */
    private String mExpressionFlag = "";
    /**
     * Whether the expression we're parsing is raw (should not be escaped).
     */
    private boolean mRawExpression;

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
        // Strip trailing \n. We never want it and it makes it hard
        // to have sub-templates that fit nicely within HTML. (It
        // forces nested templates to be followed by whitespace.)
        int length = template.length();
        if (length > 0 && template.charAt(length - 1) == '\n') {
            template = template.substring(0, length - 1);
        }

        // We'll write Java to this builder.
        mWriter = new StringBuilder();

        // Create our output variable.
        switch (mOutputMethod) {
            case PRINT_STREAM:
                // Not implemented. Somehow get a PrintStream instance
                // and put it into a local WRITER_NAME variable.
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

    /**
     * Reset the state for a new template line.
     */
    private void startNewLine() {
        mPreStatementBegin = mWriter.length();
        mPreStatementEnd = -1;
        mPostStatementBegin = -1;
        mPostStatementEnd = -1;
    }

    /**
     * Process an input character and advance the state machine.
     */
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
                    mRawExpression = false;
                    mState = State.EXPRESSION_HEAD;
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

            case EXPRESSION_HEAD:
                if (ch == ':') {
                    mState = State.EXPRESSION_FLAG;
                    mExpressionFlag = "";
                } else if (ch == '}') {
                    mState = State.SAW_CLOSE_BRACE;
                } else {
                    // No (more) head.
                    startExpression();
                    mWriter.append(ch);
                    mState = State.EXPRESSION;
                }
                break;

            case EXPRESSION:
                if (ch == '}') {
                    mState = State.SAW_CLOSE_BRACE;
                } else {
                    mWriter.append(ch);
                }
                break;

            case EXPRESSION_FLAG:
                if (Character.isLetterOrDigit(ch)) {
                    mExpressionFlag += ch;
                } else if (ch == ':') {
                    // End of expression, start of new flag.
                    processExpressionFlag(mExpressionFlag);
                    mExpressionFlag = "";
                } else if (Character.isWhitespace(ch)) {
                    // End of expression head.
                    processExpressionFlag(mExpressionFlag);
                    startExpression();
                    mState = State.EXPRESSION;
                    mWriter.append(ch);
                } else {
                    // XXX Error.
                    startExpression();
                    mState = State.EXPRESSION;
                    System.err.printf("Invalid character %c%n", ch);
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

    /**
     * If a line was entirely made up of whitespace, a statement,
     * then more whitespace, delete the whitespace and following newline.
     * That stuff wasn't meant to be part of the output, it was just
     * the nicest way to format the statement.
     */
    private boolean possiblyDeleteLine() {
        if (mPreStatementBegin != -1
                && mPreStatementEnd != -1
                && mPostStatementBegin != -1
                && isBlank(mPreStatementBegin, mPreStatementEnd)
                && isBlank(mPostStatementBegin, mPostStatementEnd)) {

            // We need these guards against the possibility that
            // the "being" is past the end of the string. The delete()
            // method really should recognize that if begin == length == end
            // then there's nothing to do, but it throws.
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

    /**
     * Return whether the span between begin (inclusive) and end (exclusive) of
     * mWriter is made up of spaces and tabs.
     */
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
        if (!mRawExpression) {
            mWriter.append(
                    "org.apache.commons.lang3.StringEscapeUtils.escapeHtml3(String.valueOf(");
        }
    }

    private void endExpression() {
        if (!mRawExpression) {
            mWriter.append("))");
        }
        mWriter.append(");\n" + mIndent);
    }

    private void startStatement() {
        // Nothing.
    }

    private void endStatement() {
        // Nothing.
    }

    private void processExpressionFlag(String flag) {
        if (flag.equals("raw")) {
            mRawExpression = true;
        } else {
            System.err.printf("Invalid expression flag \"%s\".%n", flag);
        }
    }
}

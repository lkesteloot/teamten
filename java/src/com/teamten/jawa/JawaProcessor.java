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
    private String mIncludedFilename = "";
    private static enum State {
        // Parsing Java code from the top.
        NORMAL,
        // Parsing the filename of an included template.
        TEMPLATE_FILENAME,
    }
    private State mState = State.NORMAL;

    public void processFile(String inputFilename, String outputFilename)
        throws IOException {

        Reader reader = new FileReader(inputFilename);
        mState = State.NORMAL;

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

    private void processChar(char ch) throws IOException {
        switch (mState) {
            case NORMAL:
                if (ch == '`') {
                    mState = State.TEMPLATE_FILENAME;
                } else {
                    mWriter.print(ch);
                }
                break;

            case TEMPLATE_FILENAME:
                if (ch == '`') {
                    new TemplateProcessor().processFile(mIncludedFilename,
                            mWriter);
                    mState = State.NORMAL;
                    mIncludedFilename = "";
                } else {
                    mIncludedFilename += ch;
                }
                break;
        }
    }
}

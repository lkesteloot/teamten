/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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
    private PrintStream mWriter;
    // Parser state.
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

    /**
     * Process a Jawa file, storing the Java code in another file.
     */
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

    /**
     * Reset the state machine for a new input file.
     */
    private void resetState() {
        mState = State.INDENT;
        mIncludedFilename = "";
        mIndent = "";
    }

    /**
     * Process an input character.
     */
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
                    mWriter.print(new TemplateProcessor()
                            .withIndent(mIndent)
                            .processFile(mIncludedFilename));
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

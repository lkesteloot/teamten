// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;

/**
 * Processes a single Jawa file into a Java file, expanding any
 * included templates.
 */
public class JawaProcessor {
    /**
     * The character being read is at this column, 1-based.
     */
    private PrintWriter mWriter;
    private int mColumn = 1;
    private boolean mCapturingFilename = false;
    private String mIncludedFilename = "";

    public void processFile(String inputFilename, String outputFilename)
        throws IOException {

        Reader reader = new FileReader(inputFilename);

        try {
            mWriter = new PrintWriter(outputFilename);

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
        if (ch == '\n' || ch == '\r') {
            // End of line.
            if (mCapturingFilename) {
                mCapturingFilename = false;

                new TemplateProcessor().processFile(mIncludedFilename,
                        mWriter);
            } else {
                mWriter.println();
            }

            // Start the line over.
            mColumn = 1;
        } else {
            if (mCapturingFilename) {
                mIncludedFilename += ch;
            } else if (ch == '#' && mColumn == 1) {
                mCapturingFilename = true;
                mIncludedFilename = "";
            } else {
                mWriter.print(ch);
            }

            // Next character.
            mColumn++;
        }
    }
}

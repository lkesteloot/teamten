// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.IOException;
import java.io.PrintStream;

/**
 * Pre-processor for Jawa files.
 */
public class Jawa {
    private static final ThreadLocal<PrintStream> mPrintStream =
        new ThreadLocal<PrintStream>();
    public static void main(String[] args) throws IOException {
        new Jawa().run(args);
    }

    public static void reset(PrintStream printStream) {
        mPrintStream.set(printStream);
    }

    public static PrintStream getPrintStream() {
        return mPrintStream.get();
    }

    private void run(String[] args) throws IOException {
        String inputFilename = args[0];
        String outputFilename = args[1];

        new JawaProcessor().processFile(inputFilename, outputFilename);
    }
}

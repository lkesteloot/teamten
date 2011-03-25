// Copyright 2011 Lawrence Kesteloot

package com.teamten.jawa;

import java.io.IOException;

/**
 * Pre-processor for Jawa files.
 */
public class Jawa {
    public static void main(String[] args) throws IOException {
        new Jawa().run(args);
    }

    private void run(String[] args) throws IOException {
        String inputFilename = args[0];
        String outputFilename = args[1];

        new JawaProcessor().processFile(inputFilename, outputFilename);
    }
}

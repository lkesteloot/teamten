// Copyright 2011 Lawrence Kesteloot

package com.teamten.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.List;
import java.util.ArrayList;

/**
 * Utility methods for dealing with files.
 */
public class Files {
    /**
     * Returns a list of lines from the file. Newlines are not included.
     */
    public static List<String> readLines(File file) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try {
            List<String> lines = new ArrayList<String>();
            String line;

            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }

            return lines;
        } finally {
            try {
                reader.close();
            } catch (IOException e) {
                // Swallow.
            }
        }
    }

    /**
     * Return the next code point in the reader, or -1 on end of file.
     */
    public static int nextCodePoint(Reader reader) throws IOException {
        int ch = reader.read();
        if (ch != -1 && Character.isHighSurrogate((char) ch)) {
            int high = ch;
            int low = reader.read();
            if (low == -1) {
                ch = -1;
            } else {
                ch = Character.toCodePoint((char) high, (char) low);
            }
        }

        return ch;
    }
}

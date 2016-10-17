package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Represents a filled-in black rectangle (though usually a horizontal or vertical line).
 */
public class Rule extends Box {
    public Rule(long width, long height, long depth) {
        super(width, height, depth);
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        // TODO draw box.
        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // TODO draw box.
        return getHeight() + getDepth();
    }
}

package com.teamten.typeset.element;

import com.teamten.typeset.element.NonDiscardableElement;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Parent of all bookmark elements, which keep track of where certain items (like chapter titles and
 * index entries) are.
 */
public abstract class Bookmark extends NonDiscardableElement {
    @Override
    public long getWidth() {
        return 0;
    }

    @Override
    public long getHeight() {
        return 0;
    }

    @Override
    public long getDepth() {
        return 0;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        // Nothing to do.
        return 0;
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Nothing to do.
        return 0;
    }
}

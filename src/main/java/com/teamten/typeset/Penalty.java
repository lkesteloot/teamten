
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * A location where the line can be broken, but with a penalty.
 */
public class Penalty extends DiscardableElement {
    public static final long INFINITY = 10000;
    private final long mPenalty;

    public Penalty(long penalty) {
        mPenalty = penalty;
    }

    public long getPenalty() {
        return mPenalty;
    }

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

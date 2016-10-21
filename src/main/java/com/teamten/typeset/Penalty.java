
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;

/**
 * A location where the line or page can be broken, but with a penalty.
 */
public class Penalty extends DiscardableElement {
    /**
     * Prevents breaking a line or page at this location. When negated, forces a break.
     */
    public static final long INFINITY = 10000;
    private final long mPenalty;

    public Penalty(long penalty) {
        mPenalty = penalty;
    }

    /**
     * Gets the value of the penalty.
     *
     * @return the value of the penalty.
     */
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

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%sPenalty: %d%s%n", indent, mPenalty, mPenalty == INFINITY ? " (prevent break)" : mPenalty == -INFINITY ? " (force break)" : "");
    }
}

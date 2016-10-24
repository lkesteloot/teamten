
package com.teamten.typeset.element;

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
    private final boolean mEvenPageOnly;

    /**
     * Create a penalty with the given score and whether to only apply the penalty at the end of even pages.
     * When true, the penalty will be ignored if it's considered for breaking the end of an odd page. This
     * is used to force the next section to start on an odd page.
     */
    public Penalty(long penalty, boolean evenPageOnly) {
        mPenalty = penalty;
        mEvenPageOnly = evenPageOnly;
    }

    /**
     * Create a penalty with the given score that is not limited to only even pages.
     */
    public Penalty(long penalty) {
        this(penalty, false);
    }

    /**
     * Gets the value of the penalty.
     *
     * @return the value of the penalty.
     */
    public long getPenalty() {
        return mPenalty;
    }

    /**
     * Whether this penalty should only be considered at the end of even pages.
     */
    public boolean isEvenPageOnly() {
        return mEvenPageOnly;
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
        stream.printf("%sPenalty: %d%s%s%n", indent, mPenalty,
                mPenalty == INFINITY ? " (prevent break)" : mPenalty == -INFINITY ? " (force break)" : "",
                mEvenPageOnly ? " (even pages only)" : "");
    }
}

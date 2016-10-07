package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;

/**
 * Represents a discretionary line break, storing both the split and unsplit versions of the text.
 */
public class Discretionary extends NonDiscardableElement {
    public static final int HYPHEN_PENALTY = 50;
    public static final Discretionary EMPTY = new Discretionary(HBox.EMPTY, HBox.EMPTY, HBox.EMPTY, 0);
    private final HBox mPreBreak;
    private final HBox mPostBreak;
    private final HBox mNoBreak;
    private final int mPenalty;

    public Discretionary(HBox preBreak, HBox postBreak, HBox noBreak, int penalty) {
        mPreBreak = preBreak;
        mPostBreak = postBreak;
        mNoBreak = noBreak;
        mPenalty = penalty;
    }

    public HBox getPreBreak() {
        return mPreBreak;
    }

    public HBox getPostBreak() {
        return mPostBreak;
    }

    public HBox getNoBreak() {
        return mNoBreak;
    }

    public int getPenalty() {
        return mPenalty;
    }

    @Override
    public long getWidth() {
        throw new IllegalStateException("discretionary breaks don't have a size");
    }

    @Override
    public long getHeight() {
        throw new IllegalStateException("discretionary breaks don't have a size");
    }

    @Override
    public long getDepth() {
        throw new IllegalStateException("discretionary breaks don't have a size");
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        throw new IllegalStateException("discretionary elements should be not laid out horizontally");
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        throw new IllegalStateException("discretionary elements should be not laid out vertically");
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%sDiscretionary: split as \"%s\" and \"%s\" or whole as \"%s\"%n",
                indent, mPreBreak.toTextString(), mPostBreak.toTextString(), mNoBreak.toTextString());
    }
}

package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Represents a discretionary line break, storing both the split and unsplit versions of the text.
 */
public class Discretionary extends NonDiscardableElement {
    private final String mPreBreak;
    private final String mPostBreak;
    private final String mNoBreak;

    public Discretionary(String preBreak, String postBreak, String noBreak) {
        mPreBreak = preBreak;
        mPostBreak = postBreak;
        mNoBreak = noBreak;
    }

    public String getPreBreak() {
        return mPreBreak;
    }

    public String getPostBreak() {
        return mPostBreak;
    }

    public String getNoBreak() {
        return mNoBreak;
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
}

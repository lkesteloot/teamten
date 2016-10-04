package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents a kerning adjustment.
 */
public class Kern extends DiscardableElement {
    private final long mAmount;
    private final boolean mIsHorizontal;

    public Kern(long amount, boolean isHorizontal) {
        mAmount = amount;
        mIsHorizontal = isHorizontal;
    }

    /**
     * The amount to adjust. This is normally negative.
     */
    public long getAmount() {
        return mAmount;
    }

    /**
     * Whether this kern is intended to be horizontal (between letters) or vertical (between lines). It's normally
     * horizontal.
     */
    public boolean isHorizontal() {
        return mIsHorizontal;
    }

    @Override
    public long getWidth() {
        return mIsHorizontal ? mAmount : 0;
    }

    @Override
    public long getHeight() {
        return mIsHorizontal ? 0 : mAmount;
    }

    @Override
    public long getDepth() {
        return 0;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        return getHeight();
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%s%s kern: %.1fpt%n", indent, mIsHorizontal ? "Horizontal" : "Vertical", PT.fromSp(mAmount));
    }
}

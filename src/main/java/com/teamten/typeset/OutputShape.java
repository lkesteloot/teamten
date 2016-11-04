package com.teamten.typeset;

/**
 * Records the shape of the output of this element list. This is typically used to give paragraphs
 * heterogeneous line widths (e.g., hanging indents).
 */
public class OutputShape {
    private final int mFirstLength;
    private final long mFirstSize;
    private final long mFirstIndent;
    private final long mSecondSize;
    private final long mSecondIndent;

    /**
     * Creates an OutputShape object.
     *
     * @param firstLength number of lines that are of firstSize and firstIndent.
     * @param firstSize width of the first firstLength lines.
     * @param firstIndent indent of the first firstLength lines.
     * @param secondSize width of the subsequent lines.
     * @param secondIndent indent of the subsequent lines.
     */
    public OutputShape(int firstLength, long firstSize, long firstIndent, long secondSize, long secondIndent) {
        mFirstLength = firstLength;
        mFirstSize = firstSize;
        mFirstIndent = firstIndent;
        mSecondSize = secondSize;
        mSecondIndent = secondIndent;
    }

    /**
     * Homogeneous output shape with no indent.
     */
    public static OutputShape fixed(long size) {
        return new OutputShape(0, 0, 0, size, 0);
    }

    /**
     * Single line of indent.
     */
    public static OutputShape singleLine(long size, long firstIndent, long subsequentIndent) {
        return new OutputShape(1, size - firstIndent, firstIndent, size - subsequentIndent, subsequentIndent);
    }

    /**
     * Get the size for the specified (1-based) line or page.
     */
    public long getSize(int counter) {
        return counter <= mFirstLength ? mFirstSize : mSecondSize;
    }

    /**
     * Get the indent for the specified (1-based) line or page.
     */
    public long getIndent(int counter) {
        return counter <= mFirstLength ? mFirstIndent : mSecondIndent;
    }
}

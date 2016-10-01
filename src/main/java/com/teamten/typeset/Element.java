
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * An element that can be stacked horizontally to make a line.
 */
public abstract class Element {
    /**
     * Return the width of the element.
     */
    public abstract long getWidth();

    /**
     * Return the height (above the baseline) of the element.
     */
    public abstract long getHeight();

    /**
     * Return the depth (below the baseline) of the element.
     */
    public abstract long getDepth();

    /**
     * Add the element to the contents.
     *
     * @param x the left-most point of the element.
     * @param y the baseline of the element.
     * @param contents the stream to add the element to.
     * @return how much to move right afterward.
     */
    public abstract long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException;

    /**
     * Add the element to the contents.
     *
     * @param x the left-most point of the element.
     * @param y the upper-left point of the element.
     * @param contents the stream to add the element to.
     * @return how much to move down afterward.
     */
    public abstract long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException;
}

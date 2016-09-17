
package com.teamten.typeset;

/**
 * An element that can be stacked horizontally to make a line.
 */
public abstract class Element {
    /**
     * Whether this element can be replaced with a line break.
     */
    public abstract boolean canBreakLine(float lineWidth, float maxLineWidth);
}

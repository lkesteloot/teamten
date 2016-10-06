package com.teamten.typeset;

import java.util.List;

/**
 * Interface for anything that has width, height, and depth.
 */
public interface Dimensions {
    /**
     * The width of the object in scaled points.
     */
    long getWidth();

    /**
     * The distance between the baseline and the top of the object in scaled points.
     */
    long getHeight();

    /**
     * The distance between the baseline and the bottom of the object in scaled points.
     */
    long getDepth();

    /**
     * Computes the sum of the item widths and the max of the height and depth.
     */
    static <E extends Dimensions> Dimensions horizontally(List<E> items) {
        long totalWidth = 0;
        long totalHeight = 0;
        long totalDepth = 0;

        for (E item : items) {
            long width = item.getWidth();
            long height = item.getHeight();
            long depth = item.getDepth();

            totalWidth += width;
            totalHeight = Math.max(totalHeight, height);
            totalDepth = Math.max(totalDepth, depth);
        }

        return new AbstractDimensions(totalWidth, totalHeight, totalDepth);
    }
}

package com.teamten.typeset;

/**
 * Various ways to vertically align a list of elements.
 */
public enum VerticalAlignment {
    /**
     * The baseline is at the top of the first element. All elements are in the depth.
     */
    TOP,
    /**
     * The baseline matches that of the first (top-most) box.
     */
    FIRST_BOX,
    /**
     * The baseline matches that of the last (bottom-most) box.
     */
    LAST_BOX,
    /**
     * The baseline is at the bottom of the last element. All elements are in the height.
     */
    BOTTOM
}

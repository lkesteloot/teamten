package com.teamten.typeset.element;

/**
 * Interface for elements that can stretch or shrink.
 */
public interface Flexible {
    /**
     * Get the size of this element in the dimension of its flexibility.
     */
    long getSize();

    /**
     * How much this element can stretch.
     */
    Flexibility getStretch();

    /**
     * How much this element can shrink.
     */
    Flexibility getShrink();

    /**
     * Fix the element to the specified size.
     */
    Element fixed(long newSize);
}

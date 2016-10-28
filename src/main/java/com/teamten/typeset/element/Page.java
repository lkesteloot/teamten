package com.teamten.typeset.element;

import java.util.List;

/**
 * Represents a printed page, including the text and physical page number.
 */
public class Page extends VBox {
    private final int mPhysicalPageNumber;

    /**
     * The elements are listed top to bottom.
     */
    public Page(List<Element> elements, int physicalPageNumber, long shift) {
        super(elements, shift);
        mPhysicalPageNumber = physicalPageNumber;
    }

    public int getPhysicalPageNumber() {
        return mPhysicalPageNumber;
    }
}

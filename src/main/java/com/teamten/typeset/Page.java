package com.teamten.typeset;

import java.util.List;

/**
 * Represents a printed page, including the text and physical page number.
 */
public class Page extends VBox {
    private final int mPhysicalPageNumber;

    /**
     * The elements are listed top to bottom.
     */
    public Page(List<Element> elements, int physicalPageNumber) {
        super(elements);
        mPhysicalPageNumber = physicalPageNumber;
    }

    public int getPhysicalPageNumber() {
        return mPhysicalPageNumber;
    }
}

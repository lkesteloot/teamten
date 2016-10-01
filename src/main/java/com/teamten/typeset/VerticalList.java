package com.teamten.typeset;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates elements in a vertical list until the document is finished, at which point a list of
 * pages is generated.
 */
public class VerticalList {
    private final List<Element> mElements = new ArrayList<>();

    public void addElement(Element element) {
        mElements.add(element);
    }

    public List<Page> generatePages() {
        List<Page> pages = new ArrayList<>();

        // Put everything on one page.
        Page page = new Page(mElements);
        pages.add(page);

        return pages;
    }
}

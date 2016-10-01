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

    public List<Page> generatePages(long boxHeight) {
        // List of indices in mElements where each page starts.
        List<Integer> pageStartIndices = new ArrayList<>();

        // Greedily look for places to break pages.
        long total = 0;
        for (int i = 0; i < mElements.size(); i++) {
            Element element = mElements.get(i);
            long elementSize = element.getHeight() + element.getDepth();

            if (i == 0 || total + elementSize > boxHeight) {
                pageStartIndices.add(i);
                total = 0;
            }

            total += elementSize;
        }
        pageStartIndices.add(mElements.size());

        // Create the pages.
        List<Page> pages = new ArrayList<>(pageStartIndices.size() - 1);
        for (int i = 0; i < pageStartIndices.size() - 1; i++) {
            int thisElement = pageStartIndices.get(i);
            int nextElement = pageStartIndices.get(i + 1);

            Page page = new Page(mElements.subList(thisElement, nextElement));
            pages.add(page);
        }

        return pages;
    }
}

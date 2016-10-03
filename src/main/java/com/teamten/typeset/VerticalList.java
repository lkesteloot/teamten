package com.teamten.typeset;

import java.util.ArrayList;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Accumulates elements in a vertical list until the document is finished, at which point a list of
 * pages (type VBox) is generated.
 */
public class VerticalList extends ElementList {
    /**
     * The depth of the last box that was added.
     */
    private long mPreviousDepth = 0;
    private long mBaselineSkip = PT.toSp(11*1.2); // Default for 11pt font.

    @Override
    public void addElement(Element element) {
        // Add glue just before boxes so that the baselines are the right distance apart.
        if (element instanceof Box) {
            long skip = Math.max(0, mBaselineSkip - mPreviousDepth - element.getHeight());
            super.addElement(new Glue(skip, 0, 0, false));
            mPreviousDepth = element.getDepth();
        }

        super.addElement(element);
    }

    /**
     * Specify the distance between baselines. This is normally scaled by the font size,
     * for example 120% of font size. Set this between paragraphs when the font size changes.
     */
    public void setBaselineSkip(long baselineSkip) {
        mBaselineSkip = baselineSkip;
    }

    @Override
    protected Box makeOutputBox(List<Element> elements) {
        return new VBox(elements);
    }

    /**
     * Group elements vertically into pages, each of verticalSize height.
     */
    /*
    public List<VBox> generatePages(long verticalSize) {
        // List of indices in mElements where each page starts.
        List<Integer> pageStartIndices = new ArrayList<>();

        // Greedily look for places to break pages.
        long total = 0;
        for (int i = 0; i < mElements.size(); i++) {
            Element element = mElements.get(i);
            long elementSize = element.getHeight() + element.getDepth();

            if (i == 0 || total + elementSize > verticalSize) {
                pageStartIndices.add(i);
                total = 0;
            }

            total += elementSize;
        }

        // Add the end for convenience.
        pageStartIndices.add(mElements.size());

        // Create the pages.
        List<VBox> pages = new ArrayList<>(pageStartIndices.size() - 1);
        for (int i = 0; i < pageStartIndices.size() - 1; i++) {
            int thisElement = pageStartIndices.get(i);
            int nextElement = pageStartIndices.get(i + 1);

            VBox page = new VBox(mElements.subList(thisElement, nextElement));
            pages.add(page);
        }

        return pages;
    }
    */
}

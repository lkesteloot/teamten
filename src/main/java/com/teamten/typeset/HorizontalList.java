package com.teamten.typeset;

import java.util.List;

/**
 * Accumulates elements in a horizontal list until a paragraph is finished, at which point a list of
 * elements is generated and added to a vertical list.
 */
public class HorizontalList extends ElementList {
    @Override
    protected Box makeOutputBox(List<Element> elements) {
        return new HBox(elements);
    }

    @Override
    protected long getElementSize(Element element) {
        return element.getWidth();
    }
}

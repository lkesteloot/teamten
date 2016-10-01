package com.teamten.typeset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Accumulates elements in a horizontal list until a paragraph is finished, at which point a list of
 * elements is generated and added to a vertical list.
 */
public class HorizontalList {
    private final List<Element> mElements = new ArrayList<>();

    public void addElement(Element element) {
        mElements.add(element);
    }

    /**
     * Format the horizontal list and add the elements to the vertical list.
     */
    public void format(VerticalList verticalList) {
        for (Element element : mElements) {
            if (element instanceof Box) {
                HBox hbox = new HBox(Arrays.asList(element));
                verticalList.addElement(hbox);
            }
        }
    }
}

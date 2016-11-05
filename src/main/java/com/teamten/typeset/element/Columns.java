package com.teamten.typeset.element;

import com.teamten.typeset.ColumnLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * A horizontal box that contains multiple vertical columns.
 */
public class Columns extends HBox {
    public Columns(List<Element> elements, ColumnLayout columnLayout) {
        super(createColumns(elements, columnLayout), 0);
    }

    /**
     * Find the best decomposition of the elements into columns.
     */
    private static List<Element> createColumns(List<Element> elements, ColumnLayout columnLayout) {
        // Round up so that the last column has less stuff.
        int elementsPerColumn = (int) Math.ceil((double) elements.size() / columnLayout.getColumnCount());

        // Sequences of vertical boxes and glues (for the margins).
        List<Element> horizontalElements = new ArrayList<>();

        int beginIndex = 0;
        for (int i = 0; i < columnLayout.getColumnCount(); i++) {
            int endIndex = Math.min(beginIndex + elementsPerColumn, elements.size());
            VBox vbox = new VBox(elements.subList(beginIndex, endIndex));

            if (!horizontalElements.isEmpty()) {
                horizontalElements.add(new Glue(columnLayout.getMargin(), 0, 0, true));
            }

            horizontalElements.add(vbox);

            beginIndex = endIndex;
        }

        return horizontalElements;
    }
}

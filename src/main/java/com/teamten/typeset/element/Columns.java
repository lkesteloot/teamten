package com.teamten.typeset.element;

import com.teamten.typeset.ColumnLayout;
import com.teamten.typeset.ColumnVerticalList;
import com.teamten.typeset.VerticalList;

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
        // Create a vertical list with all our elements.
        ColumnVerticalList columnVerticalList = new ColumnVerticalList();
        elements.forEach(columnVerticalList::addElement);

        // Returns a sequence of vertical boxes (columns) and glues (margins).
        return columnVerticalList.formatIntoColumns(columnLayout);
    }
}

package com.teamten.typeset;

import com.teamten.typeset.element.Box;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.VBox;
import org.jetbrains.annotations.NotNull;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 * A vertical list of elements that will get broken into columns.
 */
public class ColumnVerticalList extends ElementList {
    @Override
    protected Box makeOutputBox(List<Element> elements, int counter, long shift) {
        return new VBox(elements);
    }

    @Override
    protected long getElementSize(Element element) {
        return element.getHeight() + element.getDepth();
    }

    @Override
    protected List<Element> getElementSublist(Breakpoint beginBreakpoint, Breakpoint endBreakpoint) {
        throw new NotImplementedException();
    }

    /**
     * Takes the list of elements in the vertical list and breaks them into
     * equal-sized columns.
     *
     * @return a list of columns (VBox) separated by margins (Glue).
     */
    public List<Element> formatIntoColumns(@NotNull ColumnLayout columnLayout) {
        List<Element> elements = getElements();

        /// TODO System.out.println("--------------------------------");
        /// Element.println(elements, System.out, "");

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

package com.teamten.typeset.element;

import com.teamten.typeset.ColumnLayout;
import com.teamten.typeset.ColumnVerticalList;
import com.teamten.typeset.VerticalAlignment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A horizontal box that contains multiple vertical columns.
 */
public class Columns extends HBox implements Flexible {
    private final Flexibility mStretch;
    private final Flexibility mShrink;

    public Columns(List<Element> elements, ColumnLayout columnLayout) {
        super(createColumns(elements, columnLayout));

        TotalFlexibility stretch = new TotalFlexibility();
        TotalFlexibility shrink = new TotalFlexibility();

        // Go through the elements of this horizontal box, picking out the columns.
        for (Element element : getElements()) {
            if (element instanceof VBox) {
                VBox vbox = (VBox) element;

                for (Element subelement : vbox.getElements()) {
                    if (subelement instanceof Flexible) {
                        Flexible flexible = (Flexible) subelement;
                        stretch.add(flexible.getStretch());
                        shrink.add(flexible.getShrink());
                    }
                }
            }
        }

        if (stretch.isInfinite() || shrink.isInfinite()) {
            // This would only work if all columns were infinitely stretchable. Don't deal with that.
            throw new IllegalStateException("columns cannot contain infinitely flexible items");
        }

        // Set the flexibility to the average of the flexibilities of the columns.
        mStretch = new Flexibility(stretch.getAmount()/columnLayout.getColumnCount(), false);
        mShrink = new Flexibility(shrink.getAmount()/columnLayout.getColumnCount(), false);
    }

    @Override
    public long getSize() {
        return getVerticalSize();
    }

    @Override
    public Flexibility getStretch() {
        return mStretch;
    }

    @Override
    public Flexibility getShrink() {
        return mShrink;
    }

    @Override
    public Element fixed(long newSize) {
        System.out.println("Column fixing:");
        System.out.printf("    Before vertical size: %,d\n", getVerticalSize());
        System.out.printf("    Fixing to: %,d\n", newSize);
        List<Element> newElements = getElements().stream()
                .map(element -> {
                    // Fix the columns.
                    if (element instanceof VBox) {
                        System.out.printf("        VBox fixed from: %,d\n", element.getVerticalSize());
                        VBox vbox = (VBox) element;
                        element = vbox.fixed(newSize, VerticalAlignment.FIRST_BOX);
                        System.out.printf("        VBox fixed to: %,d\n", element.getVerticalSize());
                    }

                    return element;
                })
                .collect(Collectors.toList());

        // Create an HBox now that we're fixed. We could instead create a Columns if that mattered, but currently
        // Columns adds nothing once fixed.
        HBox hbox = new HBox(newElements);
        System.out.printf("    After vertical size: %,d\n", hbox.getVerticalSize());
        return hbox;
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

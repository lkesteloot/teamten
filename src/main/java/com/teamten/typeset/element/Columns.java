/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.typeset.element;

import com.teamten.typeset.AbstractDimensions;
import com.teamten.typeset.ColumnLayout;
import com.teamten.typeset.ColumnVerticalList;
import com.teamten.typeset.Dimensions;
import com.teamten.typeset.VerticalAlignment;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A horizontal box that contains multiple vertical columns.
 */
public class Columns extends HBox implements Flexible {
    private static final boolean DEBUG_FIXING = false;
    private final Flexibility mStretch;
    private final Flexibility mShrink;

    private Columns(List<Element> elements, long shift, Flexibility stretch, Flexibility shrink) {
        super(elements, shift);

        mStretch = stretch;
        mShrink = shrink;
    }

    public static Columns create(List<Element> elements, ColumnLayout columnLayout) {
        List<Element> columns = createColumns(elements, columnLayout);

        // Compute total flexibility.
        TotalFlexibility totalStretch = new TotalFlexibility();
        TotalFlexibility totalShrink = new TotalFlexibility();

        // Go through the elements of this horizontal box, picking out the columns.
        columns.stream()
                .filter(element -> element instanceof VBox)
                .forEach(element -> {
                    VBox vbox = (VBox) element;

                    vbox.getElements().stream()
                            .filter(subelement -> subelement instanceof Flexible)
                            .forEach(subelement -> {
                                Flexible flexible = (Flexible) subelement;
                                totalStretch.add(flexible.getStretch());
                                totalShrink.add(flexible.getShrink());
                            });
                });

        if (totalStretch.isInfinite() || totalShrink.isInfinite()) {
            // This would only work if all columns were infinitely stretchable. Don't deal with that.
            throw new IllegalStateException("columns cannot contain infinitely flexible items");
        }

        // Set the flexibility to the average of the flexibilities of the columns.
        Flexibility stretch = new Flexibility(totalStretch.getAmount()/columnLayout.getColumnCount(), false);
        Flexibility shrink = new Flexibility(totalShrink.getAmount()/columnLayout.getColumnCount(), false);

        return new Columns(columns, 0, stretch, shrink);
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
        if (DEBUG_FIXING) {
            System.out.println("Column fixing:");
            System.out.printf("    Before vertical size: %,d\n", getVerticalSize());
            System.out.printf("    Fixing to: %,d\n", newSize);
        }
        List<Element> newElements = getElements().stream()
                .map(element -> {
                    // Fix the columns.
                    if (element instanceof VBox) {
                        if (DEBUG_FIXING) {
                            System.out.printf("        VBox fixed from: %,d\n", element.getVerticalSize());
                        }
                        VBox vbox = (VBox) element;
                        element = vbox.fixed(newSize, VerticalAlignment.FIRST_BOX);
                        if (DEBUG_FIXING) {
                            System.out.printf("        VBox fixed to: %,d\n", element.getVerticalSize());
                        }
                    }

                    return element;
                })
                .collect(Collectors.toList());

        // Create an HBox now that we're fixed. We could instead create a Columns if that mattered, but currently
        // Columns adds nothing once fixed.
        HBox hbox = new HBox(newElements);
        if (DEBUG_FIXING) {
            System.out.printf("    After vertical size: %,d\n", hbox.getVerticalSize());
        }
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

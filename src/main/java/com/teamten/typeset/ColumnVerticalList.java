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

package com.teamten.typeset;

import com.teamten.typeset.element.Box;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.VBox;
import com.teamten.util.IncreasingCounter;
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
        return element.getVerticalSize();
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
        boolean debug = elements.size() > 0 && elements.get(0).toTextString().startsWith("FIRST LINE HERE");
        if (debug) {
            System.out.println("------------------------------------------");
        }

        // Find all the places that we could break a column.
        List<Breakpoint> breakpoints = findBreakpoints();

        // For each possible breakpoint, figure out the next displayed element (after the breakpoint).
        computeStartIndices(breakpoints);

        // Here we're including breakpoints[0], which we always include because we need its start index.
        int numBreaks = columnLayout.getColumnCount();

        // Go through each combination of breaks.
        long minDemerits = Long.MAX_VALUE;
        List<VBox> bestVboxes = null;
        for (int[] breaks : new IncreasingCounter(numBreaks, breakpoints.size())) {
            if (breaks[0] != 0) {
                // We're done, we always want to include breakpoint zero.
                break;
            }

            // Make the vertical boxes for the columns.
            List<VBox> vboxes = new ArrayList<>();
            long maxSize = 0;
            for (int i = 0; i < breaks.length; i++) {
                int beginIndex = breakpoints.get(breaks[i]).getStartIndex();
                int endIndex = i == breaks.length - 1 ? elements.size() : breakpoints.get(breaks[i + 1]).getIndex();

                // Get rid of the depth of the last box, so that the last line is baseline-aligned across
                // all columns.
                List<Element> subElements = elements.subList(beginIndex, endIndex);
                Dimensions dimensions = Dimensions.verticallyLastBoxAligned(subElements);
                dimensions = new AbstractDimensions(dimensions.getWidth(), dimensions.getHeight(), 0);
                VBox vbox = new VBox(subElements, dimensions, 0);
                vboxes.add(vbox);

                // Keep track of the longest column.
                long size = vbox.getVerticalSize();
                if (size > maxSize) {
                    maxSize = size;
                }
            }


            if (debug) {
                Element.println(vboxes, System.out, "    ");
            }

            // Find the sum of squares of differences between each column's size and the longest column's size.
            // Minimizing this should roughly equalize the columns.
            long demerits = 0;
            int columnNumber = vboxes.size();
            for (VBox vbox : vboxes) {
                long size = vbox.getVerticalSize();
                long difference = maxSize - size;

                // This would overflow if our difference were over 53 feet. Multiply by the column number
                // as an additional penalty so that we push shorter columns to the end.
                long demerit = difference*difference*columnNumber;
                demerits += demerit;
                if (debug) {
                    System.out.printf("    %d: + %,d = %,d\n", columnNumber, demerit, demerits);
                }

                columnNumber--;
            }

            // Keep track of most equal division of columns.
            if (demerits < minDemerits) {
                minDemerits = demerits;
                bestVboxes = vboxes;
            }
        }

        // Sequences of vertical boxes and glues (for the margins).
        List<Element> horizontalElements = new ArrayList<>();

        if (bestVboxes == null) {
            // Degenerate case, probably only one element.
            VBox vbox = new VBox(elements);
            horizontalElements.add(vbox);
        } else {
            // Glue together the boxes. Keep track of the actual width and ideal width so that we can
            // compensate in the margins.
            long actualWidth = 0;
            long idealWidth = 0;
            for (VBox vbox : bestVboxes) {
                if (!horizontalElements.isEmpty()) {
                    // Shrink margin if the previous columns were too large.
                    idealWidth += columnLayout.getMargin();

                    long margin = idealWidth - actualWidth;
                    if (margin > 0) {
                        horizontalElements.add(new Glue(margin, 0, 0, true));
                    }

                    actualWidth += margin;
                }

                horizontalElements.add(vbox);

                actualWidth += vbox.getWidth();
                idealWidth += columnLayout.getColumnWidth();
            }
        }

        return horizontalElements;
    }
}

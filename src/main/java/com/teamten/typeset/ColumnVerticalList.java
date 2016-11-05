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

        // Find all the places that we could break a column.
        List<Breakpoint> breakpoints = findBreakpoints();

        // For each possible breakpoint, figure out the next displayed element (after the breakpoint).
        computeStartIndices(breakpoints);

        // Here we're including breakpoints[0], which we always include because we need its start index.
        int numBreaks = columnLayout.getColumnCount();

        // Go through each combination of breaks.
        long minSumSquares = Long.MAX_VALUE;
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

                VBox vbox = new VBox(elements.subList(beginIndex, endIndex));
                vboxes.add(vbox);

                // Keep track of the longest column.
                long size = vbox.getHeight() + vbox.getDepth();
                if (size > maxSize) {
                    maxSize = size;
                }
            }

            // Find the sum of squares of differences between each column's size and the longest column's size.
            // Minimizing this should roughly equalize the columns.
            long sumSquares = 0;
            for (VBox vbox : vboxes) {
                long size = vbox.getHeight() + vbox.getDepth();
                long difference = maxSize - size;

                // This would overflow if our difference were over 53 feet.
                sumSquares += difference*difference;
            }

            // Keep track of most equal division of columns.
            if (sumSquares < minSumSquares) {
                minSumSquares = sumSquares;
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
            // Glue together the boxes.
            for (VBox vbox : bestVboxes) {
                if (!horizontalElements.isEmpty()) {
                    horizontalElements.add(new Glue(columnLayout.getMargin(), 0, 0, true));
                }

                horizontalElements.add(vbox);
            }
        }

        return horizontalElements;
    }
}

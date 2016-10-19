package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a page in the output medium.
 */
public class VBox extends Box {
    private final List<Element> mElements;

    /**
     * The elements are listed top to bottom.
     */
    public VBox(List<Element> elements) {
        super(determineVBoxDimensions(elements));
        mElements = elements;
    }

    public List<Element> getElements() {
        return mElements;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        /// drawDebugRectangle(contents, x, y);

        // The layOutVertically() method expects the upper-left, not the baseline.
        y += getHeight();

        // Lay out the items vertically.
        for (Element element : mElements) {
            long advanceY = element.layOutVertically(x, y, contents);
            y -= advanceY;
        }

        return getWidth();
    }

    @Override
    public void visit(Consumer<Element> consumer) {
        super.visit(consumer);
        mElements.forEach((element) -> {
            element.visit(consumer);
        });
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.println(indent + "VBox " + getDimensionString() + ":");
        Element.println(mElements, stream, indent + "    ");
    }

    /**
     * Computes the size of the VBox. The width is the max of the widths. The height is the
     * sum of all the heights and depths except for the last box's depth. The depth is the
     * last box's depth.
     *
     * @param elements the elements top to bottom.
     */
    private static Dimensions determineVBoxDimensions(List<Element> elements) {
        long boxWidth = 0;
        long boxHeight = 0;
        long boxDepth = 0;

        Element lastBox = null;
        for (Element element : elements) {
            long width = element.getWidth();
            long height = element.getHeight();
            long depth = element.getDepth();

            boxWidth = Math.max(boxWidth, width);
            boxHeight += height + depth;

            if (element instanceof Box) {
                // Depth is the depth of the last box.
                lastBox = element;
            }
        }

        // Compute the overall box depth.
        if (lastBox != null) {
            long depth = lastBox.getDepth();
            boxDepth = depth;
            boxHeight -= depth;
        }

        return new AbstractDimensions(boxWidth, boxHeight, boxDepth);
    }
}

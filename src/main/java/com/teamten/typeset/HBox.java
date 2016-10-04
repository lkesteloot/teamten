package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Horizontal sequence of elements.
 */
public class HBox extends Box {
    private final List<Element> mElements;

    public HBox(List<Element> elements) {
        super(determineBoxDimensions(elements));
        mElements = elements;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        drawDebugRectangle(contents, x, y);

        for (Element element : mElements) {
            long advanceX = element.layOutHorizontally(x, y, contents);
            x += advanceX;
        }

        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Skip down our height so that "y" points to our baseline.
        y -= getHeight();

        // Lay out the elements horizontally.
        layOutHorizontally(x, y, contents);

        // Our height is the combined height and depth.
        return getHeight() + getDepth();
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.println(indent + "HBox " + getDimensionString() + ":");
        for (Element element : mElements) {
            element.println(stream, indent + "    ");
        }
    }

    /**
     * Computes the sum of the elements widths and the max of the height and depth.
     */
    private static BoxDimensions determineBoxDimensions(List<Element> elements) {
        long boxWidth = 0;
        long boxHeight = 0;
        long boxDepth = 0;

        for (Element element : elements) {
            long width = element.getWidth();
            long height = element.getHeight();
            long depth = element.getDepth();

            boxWidth += width;
            boxHeight = Math.max(boxHeight, height);
            boxDepth = Math.max(boxDepth, depth);
        }

        return new BoxDimensions(boxWidth, boxHeight, boxDepth);
    }
}

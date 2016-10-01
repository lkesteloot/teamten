package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
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
        for (Element element : mElements) {
            long advanceX = element.layOutHorizontally(x, y, contents);
            x += advanceX;
        }

        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Lay out our elements horizontally.

        // Skip down our height so that "y" points to our baseline.
        y -= getHeight();

        // Lay out the elements horizontally.
        try {
            layOutHorizontally(x, y, contents);
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Our height is the combined height and depth.
        return getHeight() + getDepth();
    }

    private static BoxDimensions determineBoxDimensions(List<Element> elements) {
        long boxWidth = 0;
        long boxHeight = 0;
        long boxDepth = 0;

        for (Element element : elements) {
            long width = element.getWidth();
            long height = element.getHeight();
            long depth = element.getDepth();

            boxWidth = Math.max(boxWidth, width);
            boxHeight = Math.max(boxHeight, height);
            boxDepth = Math.max(boxDepth, depth);
        }

        return new BoxDimensions(boxWidth, boxHeight, boxDepth);
    }
}

package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.List;

/**
 * Horizontal sequence of elements.
 */
public class HBox extends Box {
    public static final HBox EMPTY = new HBox(Collections.emptyList());
    private final List<Element> mElements;

    public HBox(List<Element> elements) {
        super(Dimensions.horizontally(elements));
        mElements = elements;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        /// drawDebugRectangle(contents, x, y);

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
        Element.println(stream, indent + "    ", mElements);
    }

    @Override
    public String toTextString() {
        return Element.toTextString(mElements);
    }
}

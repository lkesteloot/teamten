package com.teamten.typeset;

import java.io.PrintStream;
import java.util.List;

/**
 * Represents a page in the output medium.
 */
public class VBox extends Box {
    private final List<Element> mElements;

    public VBox(List<Element> elements) {
        super(determineVBoxDimensions(elements));
        mElements = elements;
    }

    public List<Element> getElements() {
        return mElements;
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.println(indent + "VBox " + getDimensionString() + ":");
        for (Element element : mElements) {
            element.println(stream, indent + "    ");
        }
    }

    /**
     * Computes the size of the VBox. The width is the max of the widths. The height is the
     * sum of all the heights and depths except for the last box's depth. The depth is the
     * last box's depth.
     */
    private static BoxDimensions determineVBoxDimensions(List<Element> elements) {
        long boxWidth = 0;
        long boxHeight = 0;
        long boxDepth = 0;

        for (Element element : elements) {
            long width = element.getWidth();
            long height = element.getHeight();
            long depth = element.getDepth();

            boxWidth = Math.max(boxWidth, width);
            boxHeight += height + depth;

            if (element instanceof Box) {
                // Depth is the depth of the last box.
                boxDepth = depth;
                boxHeight -= depth;
            }
        }

        return new BoxDimensions(boxWidth, boxHeight, boxDepth);
    }

}

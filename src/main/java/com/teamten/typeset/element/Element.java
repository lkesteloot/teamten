
package com.teamten.typeset.element;

import com.teamten.typeset.Dimensions;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.function.Consumer;

/**
 * An element that can be stacked horizontally to make a line or vertically to make a page.
 */
public abstract class Element implements Dimensions {
    protected final boolean DRAW_DEBUG = true;

    /**
     * Return the width of the element.
     */
    public abstract long getWidth();

    /**
     * Return the height (above the baseline) of the element.
     */
    public abstract long getHeight();

    /**
     * Return the depth (below the baseline) of the element.
     */
    public abstract long getDepth();

    /**
     * Add the element to the contents.
     *
     * @param x the left-most point of the element.
     * @param y the baseline of the element.
     * @param contents the stream to add the element to.
     * @return how much to move right afterward.
     */
    public abstract long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException;

    /**
     * Calls the consumer on this element and all child elements in pre-order. The default implementation
     * just calls the consumer on this element.
     */
    public void visit(Consumer<Element> consumer) {
        consumer.accept(this);
    }

    /**
     * Add the element to the contents.
     *
     * @param x the left-most point of the element.
     * @param y the upper-left point of the element.
     * @param contents the stream to add the element to.
     * @return how much to move down afterward.
     */
    public abstract long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException;

    /**
     * Pretty prints the element to the PrintWriter with the given indent. The method must print its own newline.
     */
    public void println(PrintStream stream, String indent) {
        // Default implementation just uses toString().
        stream.print(indent);
        stream.println(toString());
    }

    /**
     * Convenience method for implementing {@link #println(PrintStream, String)} from a list of elements.
     */
    public static void println(Iterable<Element> elements, PrintStream stream, String indent) {
        for (Element element : elements) {
            element.println(stream, indent);
        }
    }

    /**
     * Return a text version of the element, ideally containing only the text of the element and its children.
     */
    public String toTextString() {
        return "";
    }

    /**
     * Convenience method for implementing {@link #toTextString()} from a list of elements.
     */
    protected static String toTextString(List<Element> elements) {
        StringBuilder builder = new StringBuilder();

        for (Element element : elements) {
            builder.append(element.toTextString());
        }

        return builder.toString();
    }
}

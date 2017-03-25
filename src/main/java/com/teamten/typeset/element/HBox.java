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

import com.teamten.font.SizedFont;
import com.teamten.typeset.Dimensions;
import com.teamten.typeset.Chunk;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

/**
 * Horizontal sequence of elements.
 */
public class HBox extends Box {
    private final List<Element> mElements;

    protected HBox(List<Element> elements, Dimensions dimensions, long shift) {
        super(dimensions, shift);
        mElements = elements;
    }

    public HBox(List<Element> elements, long shift) {
        this(elements, Dimensions.horizontally(elements), shift);
    }

    public HBox(List<Element> elements) {
        this(elements, 0);
    }

    /**
     * Make a new HBox that's forced to be the specified width.
     */
    public static HBox ofWidth(List<Element> elements, long width) {
        Chunk chunk = Chunk.create(elements, width, -1, false, false, Element::getWidth);
        elements = chunk.fixed();
        return new HBox(elements, 0);
    }

    /**
     * Make a new HBox with the content left-aligned in a box of the specified width.
     */
    public static HBox leftAligned(Element element, long width) {
        return ofWidth(Arrays.asList(element, Glue.infiniteHorizontal()), width);
    }

    /**
     * Make a new HBox with the content centered in a box of the specified width.
     */
    public static HBox centered(Element element, long width) {
        return ofWidth(Arrays.asList(Glue.infiniteHorizontal(), element, Glue.infiniteHorizontal()), width);
    }

    /**
     * Make a new HBox with the content right-aligned in a box of the specified width.
     */
    public static HBox rightAligned(Element element, long width) {
        return ofWidth(Arrays.asList(Glue.infiniteHorizontal(), element), width);
    }

    /**
     * Do not modify this list.
     */
    public List<Element> getElements() {
        return mElements;
    }

    /**
     * Whether this horizontal box is empty, i.e., has no elements in it.
     */
    public boolean isEmpty() {
        return mElements.isEmpty();
    }

    /**
     * If the HBox contains only one element, and this element is a Text, then returns the text
     * of this Text. If the HBox is empty, returns an empty string.
     *
     * @throws IllegalArgumentException if one of the above conditions do not hold.
     */
    public String getOnlyString() {
        if (mElements.isEmpty()) {
            return "";
        }

        if (mElements.size() != 1) {
            throw new IllegalArgumentException("HBox must contain exactly one element");
        }

        Element element = mElements.get(0);
        if (!(element instanceof Text)) {
            throw new IllegalArgumentException("element must be Text");
        }

        return ((Text) element).getText();
    }

    /**
     * Make an HBox that contains only a Text with the given string, font, and font size.
     */
    public static HBox makeOnlyString(String text, SizedFont font) {
        return new HBox(Arrays.asList(new Text(text, font)));
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        /// drawDebugRectangle(contents, x, y);

        for (Element element : mElements) {
            long shift = element instanceof Box ? ((Box) element).getShift() : 0;
            long advanceX = element.layOutHorizontally(x, y + shift, contents);
            x += advanceX;
        }

        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Skip down our height so that "y" points to our baseline.
        y -= getHeight();

        // Lay out the elements horizontally.
        layOutHorizontally(x + getShift(), y, contents);

        // Our height is the combined height and depth.
        return getVerticalSize();
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
        stream.println(indent + "HBox " + getDimensionString() + ":");
        Element.println(mElements, stream, indent + "    ");
    }

    @Override
    public String toTextString() {
        return Element.toTextString(mElements);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HBox hBox = (HBox) o;

        return mElements.equals(hBox.mElements);

    }

    @Override
    public int hashCode() {
        return mElements.hashCode();
    }
}

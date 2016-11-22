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

import com.teamten.typeset.Chunk;
import com.teamten.typeset.Dimensions;
import com.teamten.typeset.VerticalAlignment;
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
    public VBox(List<Element> elements, Dimensions dimensions, long shift) {
        super(dimensions, shift);
        mElements = elements;
    }

    /**
     * The elements are listed top to bottom.
     */
    public VBox(List<Element> elements, long shift) {
        this(elements, Dimensions.verticallyLastBoxAligned(elements), shift);
    }

    /**
     * The elements are listed top to bottom.
     */
    public VBox(List<Element> elements) {
        this(elements, 0);
    }

    public List<Element> getElements() {
        return mElements;
    }

    /**
     * Return a new VBox with all the elements stretches or shrunk to be {@code newSize}.
     */
    public VBox fixed(long newSize, VerticalAlignment verticalAlignment) {
        // Figure out how well our existing elements fit in this new size.
        Chunk chunk = Chunk.create(getElements(), newSize, getVerticalSize(), false, Element::getVerticalSize);

        // Stretch or shrink them to fit.
        List<Element> fixedElements = chunk.fixed();

        // Package them in a vertical box.
        return new VBox(fixedElements, Dimensions.vertically(fixedElements, verticalAlignment), 0);
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
}

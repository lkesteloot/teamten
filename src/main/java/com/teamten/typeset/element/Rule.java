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

import com.teamten.typeset.PdfUtil;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

/**
 * Represents a filled-in black rectangle (though usually a horizontal or vertical line).
 */
public class Rule extends Box {
    public Rule(long width, long height, long depth) {
        super(width, height, depth);
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        PdfUtil.drawSolidRectangle(contents, x, y - getDepth(), getWidth(), getHeight());

        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        PdfUtil.drawSolidRectangle(contents, x, y - getHeight() - getDepth(), getWidth(), getHeight());

        return getVerticalSize();
    }
}

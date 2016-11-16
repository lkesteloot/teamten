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

package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Static methods for helping draw into PDF files.
 */
public class PdfUtil {
    /**
     * Draw a light gray rectangle at the specified scaled point coordinates.
     *
     * @param x lower-left X coordinate.
     * @param y lower-left Y coordinate.
     */
    public static void drawDebugRectangle(PDPageContentStream contents, long x, long y, long width, long height) throws IOException {
        contents.addRect(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y), PT.fromSpAsFloat(width), PT.fromSpAsFloat(height));
        contents.setStrokingColor(0.90);
        contents.stroke();
    }

    /**
     * Draw a solid black rectangle at the specified scaled point coordinates.
     *
     * @param x lower-left X coordinate.
     * @param y lower-left Y coordinate.
     */
    public static void drawSolidRectangle(PDPageContentStream contents, long x, long y, long width, long height) throws IOException {
        contents.addRect(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y), PT.fromSpAsFloat(width), PT.fromSpAsFloat(height));
        contents.fill();
    }
}

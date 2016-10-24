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

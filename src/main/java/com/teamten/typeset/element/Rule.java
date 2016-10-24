package com.teamten.typeset.element;

import com.teamten.typeset.PdfUtil;
import com.teamten.typeset.element.Box;
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

        return getHeight() + getDepth();
    }
}

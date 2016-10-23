package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.File;
import java.io.IOException;

/**
 * A font manager for fonts loaded by PdfBox.
 */
public class PdfBoxFontManager extends FontManager {
    /**
     * A font manager that creates new PdfBox fonts for the specified PDDocument.
     */
    public PdfBoxFontManager(PDDocument pdDoc) {
        super((File file) -> {
            try {
                return new PdfBoxFont(pdDoc, file);
            } catch (IOException e) {
                // I'd prefer an IOException, but the Function interface doesn't permit it.
                throw new IllegalArgumentException("cannot load font " + file, e);
            }
        });
    }
}

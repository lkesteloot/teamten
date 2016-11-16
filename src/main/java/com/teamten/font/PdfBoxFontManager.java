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

package com.teamten.font;

import org.apache.pdfbox.pdmodel.PDDocument;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * A font manager for fonts loaded by PdfBox.
 */
public class PdfBoxFontManager extends FontManager {
    private static final double SMALL_CAPS_SIZE = 0.8;

    /**
     * A font manager that creates new PdfBox fonts for the specified PDDocument.
     */
    public PdfBoxFontManager(PDDocument pdDoc) {
        super((typefaceVariant) -> {
            try {
                // Get the font that maps to this typeface and variant.
                FontName fontName = typefaceVariant.getFontName();

                if (fontName == null) {
                    // Fake small caps if we can.
                    if (typefaceVariant.getFontVariant() == FontVariant.SMALL_CAPS) {
                        // Base it on regular.
                        fontName = typefaceVariant.getTypeface().get(FontVariant.REGULAR);
                        if (fontName == null) {
                            throw new IllegalArgumentException("no regular font to make small caps with");
                        }

                        // Load regular and fake small caps with it.
                        Font regularFont = new PdfBoxFont(pdDoc, fontName.getFile());
                        return new SmallCapsFont(regularFont, SMALL_CAPS_SIZE);
                    } else {
                        // Don't know how to fake other variants.
                        throw new IllegalArgumentException("no font for " + typefaceVariant);
                    }
                } else {
                    // Load the font file normally.
                    return new PdfBoxFont(pdDoc, fontName.getFile());
                }
            } catch (IOException e) {
                // I'd prefer an IOException, but the Function interface doesn't permit it.
                throw new UncheckedIOException("cannot load font " + typefaceVariant, e);
            }
        });
    }
}

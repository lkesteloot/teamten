
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.IOException;

/**
 * Utility class for testing PDF things.
 */
public class PdfTest {
    public static void main(String[] args) throws IOException {
        PDDocument pdf = new PDDocument();
        PDPage page = new PDPage();
        pdf.addPage(page);
        PDPageContentStream contents = new PDPageContentStream(pdf, page);

        String text = "The quick brown fox jumps over the lazy dog.";
        /// PDFont font = PDType1Font.TIMES_ROMAN;
        PDFont font = PDType1Font.HELVETICA;
        float fontSize = 14;

        // All together.
        contents.beginText();
        contents.setFont(font, fontSize);
        contents.newLineAtOffset(100, 700);
        contents.showText(text);
        contents.endText();

        // Separately.
        float x = 100;
        for (int i = 0; i < text.length(); i++) {
            String letter = text.substring(i, i + 1);

            contents.beginText();
            contents.setFont(font, fontSize);
            contents.newLineAtOffset(x, 690);
            contents.showText(letter);
            contents.endText();

            x += font.getStringWidth(letter) / 1000 * fontSize;
        }

        contents.close();

        pdf.save(args[0]);
    }
}


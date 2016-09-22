
package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

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

        String text = "The quick Va Vo Vu V. brown fox jumps over the lazy AV dog.";
        Font font = new Font(pdf, "Times New Roman.ttf");
        float fontSize = 14;

        // All together.
        float y = 700;
        contents.beginText();
        contents.setFont(font.getPdFont(), fontSize);
        contents.newLineAtOffset(100, y);
        contents.showText(text);
        contents.endText();

        // Separately.
        float x = 100;
        y -= 15;
        for (int i = 0; i < text.length(); i++) {
            String letter = text.substring(i, i + 1);

            contents.beginText();
            contents.setFont(font.getPdFont(), fontSize);
            contents.newLineAtOffset(x, y);
            contents.showText(letter);
            contents.endText();

            x += font.getPdFont().getStringWidth(letter) / 1000 * fontSize;
        }

        // With kerning.
        x = 100;
        y -= 15;
        char previousCh = 0;
        for (int i = 0; i < text.length(); i++) {
            String letter = text.substring(i, i + 1);
            char ch = letter.charAt(0);

            if (previousCh != 0) {
                x += font.getKerning(previousCh, ch) / 1000 * fontSize;
            }

            contents.beginText();
            contents.setFont(font.getPdFont(), fontSize);
            contents.newLineAtOffset(x, y);
            contents.showText(letter);
            contents.endText();

            x += font.getPdFont().getStringWidth(letter) / 1000 * fontSize;

            previousCh = ch;
        }

        contents.close();

        pdf.save(args[0]);
    }
}



package com.teamten.typeset;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.File;
import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

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
        PdfBoxFont font = new PdfBoxFont(pdf, new File("/Library/Fonts/Times New Roman.ttf"));
        double fontSize = 14;

        // All together.
        long y = PT.toSp(700);
        contents.beginText();
        contents.setFont(font.getPdFont(), (float) fontSize);
        contents.newLineAtOffset(100, PT.fromSpAsFloat(y));
        contents.showText(text);
        contents.endText();

        // Separately.
        long x = PT.toSp(100);
        y -= PT.toSp(15);
        for (int i = 0; i < text.length(); i++) {
            String letter = text.substring(i, i + 1);

            contents.beginText();
            contents.setFont(font.getPdFont(), (float) fontSize);
            contents.newLineAtOffset(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y));
            contents.showText(letter);
            contents.endText();

            x += PT.toSp(font.getPdFont().getStringWidth(letter) / 1000 * fontSize);
        }

        // With kerning.
        x = PT.toSp(100);
        y -= PT.toSp(15);
        char previousCh = 0;
        for (int i = 0; i < text.length(); i++) {
            String letter = text.substring(i, i + 1);
            char ch = letter.charAt(0);

            if (previousCh != 0) {
                x += font.getKerning(previousCh, ch, fontSize);
            }

            contents.beginText();
            contents.setFont(font.getPdFont(), (float) fontSize);
            contents.newLineAtOffset(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y));
            contents.showText(letter);
            contents.endText();

            x += PT.toSp(font.getPdFont().getStringWidth(letter) / 1000 * fontSize);

            previousCh = ch;
        }

        contents.close();

        pdf.save(args[0]);
    }
}


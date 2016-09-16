
package com.teamten.typeset;

import com.teamten.markdown.Block;
import com.teamten.markdown.Doc;
import com.teamten.markdown.MarkdownParser;
import com.teamten.markdown.Span;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Converts a document to a PDF.
 */
public class Typesetter {
    private static final int DPI = 72;

    public static void main(String[] args) throws IOException {
        InputStream inputStream = new FileInputStream(args[0]);
        MarkdownParser parser = new MarkdownParser();
        Doc doc = parser.parse(inputStream);
        Typesetter typesetter = new Typesetter();
        PDDocument pdf = typesetter.typeset(doc);
        pdf.save(args[1]);
    }

    public PDDocument typeset(Doc doc) throws IOException {
        PDDocument pdf = new PDDocument();

        PDPage page = new PDPage();
        pdf.addPage(page);

        int pageWidth = 6*DPI;
        int pageHeight = 9*DPI;
        int pageMargin = 1*DPI;
        page.setMediaBox(new PDRectangle(pageWidth, pageHeight));

        PDPageContentStream contents = new PDPageContentStream(pdf, page);

        float y = pageHeight - pageMargin;
        for (Block block : doc.getBlocks()) {
            PDFont font;
            float fontSize;

            switch (block.getBlockType()) {
                case BODY:
                default:
                    font = PDType1Font.TIMES_ROMAN;
                    fontSize = 12;
                    break;

                case PART_HEADER:
                    font = PDType1Font.TIMES_BOLD;
                    fontSize = 36;
                    break;

                case CHAPTER_HEADER:
                    font = PDType1Font.TIMES_BOLD;
                    fontSize = 24;
                    break;
            }

            float leading = fontSize*1.4f;

            Span span = block.getSpans().get(0);
            String text = span.getText();

            // Replace non-break space with normal space for rendering.
            text = text.replace("\u00A0", " ");

            y -= leading;

            contents.beginText();
            contents.setFont(font, fontSize);
            contents.newLineAtOffset(pageMargin, y);
            contents.showText(text);
            contents.endText();
        }

        contents.close();

        return pdf;
    }
}


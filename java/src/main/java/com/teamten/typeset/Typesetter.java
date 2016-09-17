
package com.teamten.typeset;

import com.google.common.base.Splitter;
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
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a document to a PDF.
 */
public class Typesetter {
    private static final int DPI = 72;
    private static final Splitter WORD_SPLITTER = Splitter.on(" ").
        omitEmptyStrings().trimResults();

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

        float pageWidth = 6*DPI;
        float pageHeight = 9*DPI;
        float pageMargin = 1*DPI;
        page.setMediaBox(new PDRectangle(pageWidth, pageHeight));

        PDPageContentStream contents = new PDPageContentStream(pdf, page);

        // Draw the margins.
        contents.addRect(pageMargin, pageMargin, pageWidth - 2*pageMargin, pageHeight - 2*pageMargin);
        contents.setStrokingColor(0.8);
        contents.stroke();

        float y = pageHeight - pageMargin;
        for (Block block : doc.getBlocks()) {
            PDFont font;
            float fontSize;
            boolean indentFirstLine = false;

            switch (block.getBlockType()) {
                case BODY:
                default:
                    font = PDType1Font.TIMES_ROMAN;
                    fontSize = 11;
                    indentFirstLine = true;
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
            float interParagraphSpacing = leading/3;
            float firstLineSpacing = indentFirstLine ? fontSize*2 : 0;

            Span span = block.getSpans().get(0);
            String text = span.getText();
            List<String> words = WORD_SPLITTER.splitToList(text);
            List<Element> elements = new ArrayList<>();

            // Convert words to elements (boxes and glue).
            elements.add(new Glue(firstLineSpacing, getLastElement(elements)));
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                boolean isLastWord = i == words.size() - 1;

                // Replace non-break space with normal space for rendering.
                // XXX not right, should be glue (preceded by penalty 1000) so
                // that it will stretch.
                word = word.replace("\u00A0", " ");

                elements.add(new Box(getTextWidth(font, fontSize, word), word));
                if (isLastWord) {
                    elements.add(new Glue(0, getLastElement(elements)));
                    elements.add(new Penalty(0, -Penalty.INFINITY));
                } else {
                    elements.add(new Glue(getTextWidth(font, fontSize, " "), getLastElement(elements)));
                }
            }

            // Figure out where the breaks are. These are indices of "elements"
            // that should be replaced with a line break.
            List<Integer> breaks = new ArrayList<>();

            float lineWidth = 0;
            float maxLineWidth = pageWidth - 2*pageMargin;
            int lastBreakable = -1;
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);

                if (element instanceof Box) {
                    Box box = (Box) element;
                    System.out.printf("%g + %g > %g (%s)%n", lineWidth, box.getWidth(), maxLineWidth, box.getText());
                    if (lineWidth + box.getWidth() > maxLineWidth && lastBreakable != -1) {
                        breaks.add(lastBreakable);
                        i = lastBreakable;
                        lastBreakable = -1;
                        lineWidth = 0;
                    } else {
                        lineWidth += box.getWidth();
                    }
                } else if (element instanceof Glue) {
                    Glue glue = (Glue) element;
                    // XXX wrong, should be able to break here.
                    if (lineWidth + glue.getWidth() > maxLineWidth && lastBreakable != -1 && false) {
                        breaks.add(lastBreakable);
                        lastBreakable = -1;
                        lineWidth = 0;
                    } else {
                        lineWidth += glue.getWidth();
                    }
                } else if (element instanceof Penalty) {
                    Penalty penalty = (Penalty) element;
                    if (penalty.getPenalty() == -Penalty.INFINITY) {
                        breaks.add(i);
                        lastBreakable = -1;
                        lineWidth = 0;
                    }
                }

                if (element.canBreakLine()) {
                    lastBreakable = i;
                }
            }

            // Draw the words.
            int lineStart = 0;
            for (int lineEnd : breaks) {
                y -= leading;
                float x = pageMargin;

                for (int i = lineStart; i < lineEnd; i++) {
                    Element element = elements.get(i);

                    if (element instanceof Box) {
                        Box box = (Box) element;
                        contents.beginText();
                        contents.setFont(font, fontSize);
                        contents.newLineAtOffset(x, y);
                        contents.showText(box.getText());
                        contents.endText();
                        x += box.getWidth();
                    } else if (element instanceof Glue) {
                        Glue glue = (Glue) element;
                        x += glue.getWidth();
                    }
                }

                lineStart = lineEnd + 1;
            }

            y -= interParagraphSpacing;
        }

        contents.close();

        return pdf;
    }

    private static Element getLastElement(List<Element> elements) {
        return elements.isEmpty() ? null : elements.get(elements.size() - 1);
    }

    private float getTextWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }
}


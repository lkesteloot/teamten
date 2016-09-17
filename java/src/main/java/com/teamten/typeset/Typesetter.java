
package com.teamten.typeset;

import com.google.common.base.Splitter;
import com.teamten.markdown.Block;
import com.teamten.markdown.Doc;
import com.teamten.markdown.MarkdownParser;
import com.teamten.markdown.Span;
import com.teamten.markdown.BlockType;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import com.teamten.hyphen.HyphenDictionary;

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

        float pageWidth = 6*DPI;
        float pageHeight = 9*DPI;
        float pageMargin = 1*DPI;

        PDPageContentStream contents = null;
        float y = 0;

        HyphenDictionary hyphenDictionary = HyphenDictionary.fromDic("hyph_fr.dic");

        BlockType previousBlockType = null;

        for (Block block : doc.getBlocks()) {
            PDFont font;
            float fontSize;
            boolean indentFirstLine = false;

            switch (block.getBlockType()) {
                case BODY:
                default:
                    font = PDType1Font.TIMES_ROMAN;
                    fontSize = 11;
                    indentFirstLine = previousBlockType == BlockType.BODY;
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
            float interParagraphSpacing = leading/4;
            float firstLineSpacing = indentFirstLine ? fontSize*2 : 0;

            Span span = block.getSpans().get(0);
            String text = span.getText();
            List<String> words = WORD_SPLITTER.splitToList(text);

            // Convert words to elements (boxes, glue, and penalty).
            List<Element> elements = wordsToElements(words, font, fontSize, firstLineSpacing, hyphenDictionary);

            // Figure out where the breaks are. These are indices of "elements"
            // that should be replaced with a line break.
            List<Integer> breaks = elementsToBreaks(elements, pageWidth, pageMargin);

            // Draw the words.
            int lineStart = 0;
            for (int lineEnd : breaks) {
                y -= leading;
                float x = pageMargin;

                if (contents == null || y < pageMargin) {
                    if (contents != null) {
                        contents.close();
                    }

                    PDPage page = new PDPage();
                    pdf.addPage(page);
                    page.setMediaBox(new PDRectangle(pageWidth, pageHeight));
                    contents = new PDPageContentStream(pdf, page);

                    // Draw the margins.
                    contents.addRect(pageMargin, pageMargin, pageWidth - 2*pageMargin, pageHeight - 2*pageMargin);
                    contents.setStrokingColor(0.8);
                    contents.stroke();

                    y = pageHeight - pageMargin - leading;
                }

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

                // XXX Ugh, is there a better way to do this?
                if (lineEnd < elements.size()) {
                    Element element = elements.get(lineEnd);
                    if (element instanceof Penalty) {
                        Penalty penalty = (Penalty) element;
                        // XXX not correct if last word already ends with hyphen.
                        // In that case, make the penalty's width 0 above.
                        if (penalty.getPenalty() != -Penalty.INFINITY &&
                                penalty.getPenalty() != 0) {

                            contents.beginText();
                            contents.setFont(font, fontSize);
                            contents.newLineAtOffset(x, y);
                            contents.showText("-");
                            contents.endText();
                            x += penalty.getWidth();
                        }
                    }
                }


                lineStart = lineEnd + 1;
            }

            y -= interParagraphSpacing;
            previousBlockType = block.getBlockType();
        }

        if (contents != null) {
            contents.close();
        }

        return pdf;
    }

    /**
     * Convert words to elements (boxes, glue, and penalty).
     */
    private List<Element> wordsToElements(List<String> words, PDFont font, float fontSize,
            float firstLineSpacing, HyphenDictionary hyphenDictionary) throws IOException {

        List<Element> elements = new ArrayList<>();

        float spaceWidth = getTextWidth(font, fontSize, " ");
        float hyphenWidth = getTextWidth(font, fontSize, "-");

        if (firstLineSpacing != 0) {
            elements.add(new Glue(firstLineSpacing, getLastElement(elements)));
        }
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            boolean isLastWord = i == words.size() - 1;

            // Replace non-break space with normal space for rendering.
            // XXX not right, should be glue (preceded by penalty 1000) so
            // that it will stretch.
            word = word.replace("\u00A0", " ");

            // Hyphenate the word. XXX Here we should split the word if it
            // includes punctuation like m-dashes, etc.
            List<String> fragments = hyphenDictionary.hyphenate(word);
            /// System.out.printf("%-25s: %s%n", word, HyphenDictionary.segmentsToString(fragments));

            // Add the fragments, separated by penalties.
            for (int j = 0; j < fragments.size(); j++) {
                String fragment = fragments.get(j);
                elements.add(new Box(getTextWidth(font, fontSize, fragment), fragment));
                if (j < fragments.size() - 1) {
                    elements.add(new Penalty(hyphenWidth, Penalty.HYPHEN));
                }
            }

            if (isLastWord) {
                // End of paragraph.
                elements.add(new Glue(0, getLastElement(elements)));
                elements.add(new Penalty(0, -Penalty.INFINITY));
            } else {
                // Space between words.
                elements.add(new Glue(spaceWidth, getLastElement(elements)));
            }
        }

        return elements;
    }

    private List<Integer> elementsToBreaks(List<Element> elements, float pageWidth, float pageMargin) {

        List<Integer> breaks = new ArrayList<>();

        // Fit elements into lines.
        float lineWidth = 0;
        float maxLineWidth = pageWidth - 2*pageMargin;
        int lastBreakable = -1;
        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);

            if (element instanceof Box) {
                Box box = (Box) element;
                /*
                System.out.printf("%g + %g > %g (%s)%n", lineWidth, box.getWidth(), maxLineWidth, box.getText());
                */
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

            if (element.canBreakLine(lineWidth, maxLineWidth)) {
                lastBreakable = i;
            }
        }

        return breaks;
    }

    private static Element getLastElement(List<Element> elements) {
        return elements.isEmpty() ? null : elements.get(elements.size() - 1);
    }

    private float getTextWidth(PDFont font, float fontSize, String text) throws IOException {
        return font.getStringWidth(text) / 1000 * fontSize;
    }
}


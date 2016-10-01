
package com.teamten.typeset;

import com.google.common.base.Splitter;
import com.google.common.collect.PeekingIterator;
import com.teamten.hyphen.HyphenDictionary;
import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;
import com.teamten.markdown.Doc;
import com.teamten.markdown.MarkdownParser;
import com.teamten.markdown.Span;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Converts a document to a PDF.
 */
public class Typesetter {
    private static final File FONT_DIR = new File("/Library/Fonts");
    private static final File SYSTEM_FONT_DIR = new File("/System/Library/Fonts");
    private static final File MY_FONT_DIR = new File("/Users/lk/Dropbox/Personal/Fonts");
    private static final Splitter WORD_SPLITTER = Splitter.on(" ").
        omitEmptyStrings().trimResults();
    private static final boolean DRAW_MARGINS = true;

    public static void main(String[] args) throws IOException {
        InputStream inputStream = new FileInputStream(args[0]);
        MarkdownParser parser = new MarkdownParser();
        Doc doc = parser.parse(inputStream);
        Typesetter typesetter = new Typesetter();
        PDDocument pdf = typesetter.typeset(doc);
        pdf.save(args[1]);
    }

    public PDDocument typeset(Doc doc) throws IOException {
        PDDocument pdDoc = new PDDocument();

        // XXX Load these values from the document header.
        long pageWidth = SpaceUnit.IN.toSp(6);
        long pageHeight = SpaceUnit.IN.toSp(9);
        long pageMargin = SpaceUnit.IN.toSp(1);

        // Load fonts we'll need. Don't use the built-in fonts, they don't have ligatures.
        Font regularFont = new Font(pdDoc, new File(FONT_DIR, "Times New Roman.ttf"));
        Font boldFont = new Font(pdDoc, new File(FONT_DIR, "Times New Roman Bold.ttf"));
        boldFont = new Font(pdDoc, new File(MY_FONT_DIR, "Helvetica Neue/HelveticaNeue-UltraLight.ttf"));

        HyphenDictionary hyphenDictionary = HyphenDictionary.fromResource("fr");

        VerticalList verticalList = new VerticalList();

        BlockType previousBlockType = null;
        for (Block block : doc.getBlocks()) {
            Font font;
            float fontSize;
            boolean indentFirstLine = false;
            boolean allCaps = false;
            HorizontalList horizontalList = new HorizontalList();

            switch (block.getBlockType()) {
                case BODY:
                default:
                    font = regularFont;
                    fontSize = 11;
                    indentFirstLine = previousBlockType == BlockType.BODY;
                    break;

                case PART_HEADER:
                    font = boldFont;
                    fontSize = 18;
                    allCaps = true;
                    break;

                case CHAPTER_HEADER:
                    font = boldFont;
                    fontSize = 11;
                    allCaps = true;
                    break;
            }

            float leading = fontSize * 1.4f;
            float interParagraphSpacing = leading / 4;
            long firstLineSpacing = SpaceUnit.PT.toSp(indentFirstLine ? fontSize * 2 : 0);
            long spaceWidth = getTextWidth(font.getPdFont(), fontSize, " ");
            // Roughly copy TeX:
            Glue spaceGlue = new Glue(spaceWidth, spaceWidth / 2, spaceWidth / 3, true);

            Span span = block.getSpans().get(0);
            String text = span.getText();
            if (allCaps) {
                text = text.toUpperCase();
            }

            if (firstLineSpacing != 0) {
                horizontalList.addElement(new Box(firstLineSpacing, 0, 0));
            }

            for (int i = 0; i < text.length(); ) {
                int ch = text.codePointAt(i);

                if (ch == ' ') {
                    horizontalList.addElement(spaceGlue);
                } else if (ch == '\u00A0') {
                    // Non-break space. Precede with infinite penalty.
                    horizontalList.addElement(new Penalty(Penalty.INFINITY));
                    horizontalList.addElement(spaceGlue);
                } else {
                    int[] codePoints = new int[1];
                    codePoints[0] = ch;
                    String s = new String(codePoints, 0, 1);
                    long width = getTextWidth(font.getPdFont(), fontSize, s);
                    horizontalList.addElement(new Text(font, fontSize, s, width, 983025, 0)); // TODO
                }

                // Advance to the next code point.
                i += Character.charCount(ch);
            }

            // Add a forced break at the end of the paragraph.
            horizontalList.addElement(new Penalty(-Penalty.INFINITY));

            horizontalList.format(verticalList, pageWidth - 2*pageMargin);
            previousBlockType = block.getBlockType();
        }

        List<Page> pages = verticalList.generatePages();

        // Generate each page.
        for (Page page : pages) {
            PDPage pdPage = new PDPage();
            pdDoc.addPage(pdPage);
            pdPage.setMediaBox(new PDRectangle(
                    SpaceUnit.PT.fromSpAsFloat(pageWidth),
                    SpaceUnit.PT.fromSpAsFloat(pageHeight)));

            PDPageContentStream contents = new PDPageContentStream(pdDoc, pdPage);

            // Draw the margins for debugging.
            if (DRAW_MARGINS) {
                contents.addRect(SpaceUnit.PT.fromSpAsFloat(pageMargin),
                        SpaceUnit.PT.fromSpAsFloat(pageMargin),
                        SpaceUnit.PT.fromSpAsFloat(pageWidth - 2*pageMargin),
                        SpaceUnit.PT.fromSpAsFloat(pageHeight - 2*pageMargin));
                contents.setStrokingColor(0.9);
                contents.stroke();
            }

            // Start at top of page.
            long y = pageHeight - pageMargin;

            // Lay out each element in the page.
            for (Element element : page.getElements()) {
                long advanceY = element.layOutVertically(pageMargin, y, contents);

                y -= advanceY;
            }

            contents.close();
        }

        /*
            // Figure out where the breaks are. These are indices of "elements"
            // that should be replaced with a line break.
            List<Integer> breaks = elementsToBreaks(elements, pageWidth, pageMargin);

            // Draw the words.
            int lineStart = 0;
            for (int lineEnd : breaks) {
                y -= leading;
                float x = pageMargin;

                // Skip lineStart over glues and non-forcing penalties here.
                while (lineStart < lineEnd && skipElementAtStartOfLine(elements.get(lineStart))) {
                    lineStart++;
                }

                // Perhaps start a new page.
                if (contents == null || y < pageMargin) {
                }

                // Draw each word on this line.
                for (int i = lineStart; i < lineEnd; i++) {
                    Element element = elements.get(i);

                    if (element instanceof Box) {
                        Box box = (Box) element;
                        contents.beginText();
                        contents.setFont(font.getPdFont(), fontSize);
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
                            contents.setFont(font.getPdFont(), fontSize);
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
        */

        return pdDoc;
    }

    /**
     * Convert words to elements (boxes, glue, and penalty).
     */
    /*
    private List<Element> wordsToElements(List<String> words, Font font, float fontSize,
                                          long firstLineSpacing, HyphenDictionary hyphenDictionary) throws IOException {

        List<Element> elements = new ArrayList<>();

        float spaceWidth = getTextWidth(font.getPdFont(), fontSize, " ");
        float hyphenWidth = getTextWidth(font.getPdFont(), fontSize, "-");

        if (firstLineSpacing != 0) {
            elements.add(new Box(firstLineSpacing, ""));
        }
        for (int i = 0; i < words.size(); i++) {
            String word = words.get(i);
            boolean isLastWord = i == words.size() - 1;

            // Add ligatures.
            word = font.transformLigatures(word);

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
                long fragment = fragments.get(j);
                elements.add(new Box(getTextWidth(font.getPdFont(), fontSize, fragment), fragment));
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
                //System.out.printf("%g + %g > %g (%s)%n", lineWidth, box.getWidth(), maxLineWidth, box.getText());
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
                    // Always include forcing penalties.
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
    */

    private static Element getLastElement(List<Element> elements) {
        return elements.isEmpty() ? null : elements.get(elements.size() - 1);
    }

    /**
     * Returns the width of the text in scaled points.
     */
    private static long getTextWidth(PDFont font, float fontSize, String text) throws IOException {
        return SpaceUnit.PT.toSp(font.getStringWidth(text) / 1000 * fontSize);
    }

    /**
     * Returns whether this elements should be skipped if at the start of a line.
     * Basically we skip white-space and non-forcing penalties.
     */
    private static boolean skipElementAtStartOfLine(Element element) {
        if (element instanceof Box) {
            // Never skip boxes.
            return false;
        }

        if (element instanceof Penalty) {
            // Skip penalties except for forcing ones (end of paragraph).
            Penalty penalty = (Penalty) element;
            return penalty.getPenalty() != -Penalty.INFINITY;
        }

        // Skip the rest.
        return true;
    }
}


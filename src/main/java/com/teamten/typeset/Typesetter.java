
package com.teamten.typeset;

import com.google.common.base.Splitter;
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
import java.util.List;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Converts a document to a PDF.
 */
public class Typesetter {
    private static final File FONT_DIR = new File("/Library/Fonts");
    private static final File SYSTEM_FONT_DIR = new File("/System/Library/Fonts");
    private static final File MY_FONT_DIR = new File("/Users/lk/Dropbox/Personal/Fonts");
    private static final Splitter WORD_SPLITTER = Splitter.on(" ").
        omitEmptyStrings().trimResults();
    private static final boolean DRAW_MARGINS = false;

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
        long pageWidth = IN.toSp(6);
        long pageHeight = IN.toSp(9);
        long pageMargin = IN.toSp(1);

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
            boolean center = false;
            long marginTop = 0;
            long marginBottom = 0;
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
                    font = regularFont;
                    fontSize = 36;
                    allCaps = true;
                    // center = true;
                    marginTop = IN.toSp(1);
                    marginBottom = IN.toSp(2);
                    break;

                case CHAPTER_HEADER:
                    font = boldFont;
                    fontSize = 11;
                    allCaps = true;
                    break;
            }

            long leading = PT.toSp(fontSize * 1.4f);
            long interParagraphSpacing = leading / 4;
            long firstLineSpacing = PT.toSp(indentFirstLine ? fontSize * 2 : 0);
            long spaceWidth = getTextWidth(font.getPdFont(), fontSize, " ");
            // Roughly copy TeX:
            Glue spaceGlue = new Glue(spaceWidth, spaceWidth / 2, spaceWidth / 3, true);

            Span span = block.getSpans().get(0);
            String text = span.getText();
            if (allCaps) {
                text = text.toUpperCase();
            }

            if (marginTop != 0) {
                verticalList.addElement(new Glue(marginTop, 0, 0, false));
            }

            if (center) {
                horizontalList.addElement(new Glue(0, 1, true, 0, false, true));
            }

            if (firstLineSpacing != 0) {
                horizontalList.addElement(new Box(firstLineSpacing, 0, 0));
            }

            // Replace ligatures with Unicode values. TODO Not sure if we want to do this here with strings or
            // on the fly while we're rolling through code points.
            text = font.transformLigatures(text);

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
                    horizontalList.addElement(new Text(font, fontSize, s, width, PT.toSp(15), 0)); // TODO
                }

                // Advance to the next code point.
                i += Character.charCount(ch);
            }

            // Add a forced break at the end of the paragraph.
            horizontalList.addElement(new Glue(0, 1, true, 0, false, true));
            horizontalList.addElement(new Penalty(-Penalty.INFINITY));

            horizontalList.format(verticalList, pageWidth - 2*pageMargin);
            if (marginBottom != 0) {
                verticalList.addElement(new Glue(marginBottom, 0, 0, false));
            }
            verticalList.addElement(new Glue(interParagraphSpacing, 0, 0, false));
            previousBlockType = block.getBlockType();
        }

        List<Page> pages = verticalList.generatePages(pageHeight - 2*pageMargin);

        // Generate each page.
        for (Page page : pages) {
            PDPage pdPage = new PDPage();
            pdDoc.addPage(pdPage);
            pdPage.setMediaBox(new PDRectangle(
                    PT.fromSpAsFloat(pageWidth),
                    PT.fromSpAsFloat(pageHeight)));

            PDPageContentStream contents = new PDPageContentStream(pdDoc, pdPage);

            // Draw the margins for debugging.
            if (DRAW_MARGINS) {
                contents.addRect(PT.fromSpAsFloat(pageMargin),
                        PT.fromSpAsFloat(pageMargin),
                        PT.fromSpAsFloat(pageWidth - 2*pageMargin),
                        PT.fromSpAsFloat(pageHeight - 2*pageMargin));
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

    */

    private static Element getLastElement(List<Element> elements) {
        return elements.isEmpty() ? null : elements.get(elements.size() - 1);
    }

    /**
     * Returns the width of the text in scaled points.
     */
    private static long getTextWidth(PDFont font, float fontSize, String text) throws IOException {
        return PT.toSp(font.getStringWidth(text) / 1000 * fontSize);
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


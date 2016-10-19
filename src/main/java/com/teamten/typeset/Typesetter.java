
package com.teamten.typeset;

import com.google.common.base.Splitter;
import com.google.common.base.Stopwatch;
import com.teamten.hyphen.HyphenDictionary;
import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;
import com.teamten.markdown.Doc;
import com.teamten.markdown.MarkdownParser;
import com.teamten.markdown.Span;
import com.teamten.typeset.FontManager.FontName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Converts a document to a PDF.
 */
public class Typesetter {
    private static final boolean DRAW_MARGINS = true;
    /**
     * The maximum number of times that we'll typeset the document without it converging on a stable set
     * of page numbers.
     */
    private static final int MAX_ITERATIONS = 3;

    public static void main(String[] args) throws IOException {
        // Load and parse the Markdown file.
        Stopwatch stopwatch = Stopwatch.createStarted();
        InputStream inputStream = new FileInputStream(args[0]);
        MarkdownParser parser = new MarkdownParser();
        Doc doc = parser.parse(inputStream);
        System.out.println("Parsing: " + stopwatch);

        // Typeset the document.
        Typesetter typesetter = new Typesetter();
        PDDocument pdDoc = typesetter.typeset(doc);

        // Save the PDF.
        stopwatch = Stopwatch.createStarted();
        pdDoc.save(args[1]);
        System.out.println("Saving: " + stopwatch);
    }

    public PDDocument typeset(Doc doc) throws IOException {
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new FontManager(pdDoc);

        // TODO Load these values from the document header.
        BookLayout bookLayout = new BookLayout(IN.toSp(6), IN.toSp(9), IN.toSp(1),
                fontManager.get(Typeface.TIMES_NEW_ROMAN.regular()), 9);

        List<Page> pages = null;
        Bookmarks bookmarks = Bookmarks.empty();
        for (int pass = 0; pass < MAX_ITERATIONS; pass++) {
            System.out.printf("Pass %d:\n", pass + 1);
            Stopwatch stopwatch = Stopwatch.createStarted();
            VerticalList verticalList = docToVerticalList(doc, fontManager, bookLayout.getBodyWidth());
            System.out.println("  Horizontal: " + stopwatch);

            // Format the vertical list into pages.
            stopwatch = Stopwatch.createStarted();
            pages = verticalListToPages(verticalList, bookLayout.getBodyHeight());
            System.out.println("  Vertical: " + stopwatch);

            // Get the full list of bookmarks.
            Bookmarks newBookmarks = Bookmarks.fromPages(pages);
            newBookmarks.println(System.out);
            if (newBookmarks.equals(bookmarks)) {
                // We've converged, we can stop.
                break;
            }

            // Try again with these new bookmarks.
            bookmarks = newBookmarks;
        }
        // TODO throw if we had too many iterations.

        // Send pages to PDF.
        Stopwatch stopwatch = Stopwatch.createStarted();
        addPagesToPdf(pages, bookLayout, pdDoc);
        System.out.println("Adding to PDF: " + stopwatch);

        return pdDoc;
    }

    /**
     * Converts a DOM document to a vertical list.
     */
    private VerticalList docToVerticalList(Doc doc, FontManager fontManager, long bodyWidth) throws IOException {
        HyphenDictionary hyphenDictionary = HyphenDictionary.fromResource("fr");

        VerticalList verticalList = new VerticalList();

        BlockType previousBlockType = null;
        for (Block block : doc.getBlocks()) {
            Typeface typeface;
            float fontSize;
            boolean indentFirstLine = false;
            boolean allCaps = false;
            boolean center = false;
            boolean newPage = false;
            boolean ownPage = false;
            long marginTop = 0;
            long marginBottom = 0;
            HorizontalList horizontalList = new HorizontalList();

            switch (block.getBlockType()) {
                case BODY:
                default:
                    typeface = Typeface.TIMES_NEW_ROMAN;
                    fontSize = 11;
                    indentFirstLine = previousBlockType == BlockType.BODY;
                    break;

                case PART_HEADER:
                    typeface = Typeface.TIMES_NEW_ROMAN;
                    fontSize = 15;
                    allCaps = true;
                    center = true;
                    marginTop = IN.toSp(1.75);
                    newPage = true;
                    ownPage = true;
                    break;

                case CHAPTER_HEADER:
                    typeface = Typeface.TIMES_NEW_ROMAN;
                    fontSize = 11;
                    allCaps = true;
                    center = true;
                    marginTop = IN.toSp(0.75);
                    marginBottom = IN.toSp(0.25);
                    newPage = true;
                    break;
            }

            Font spanRegularFont = fontManager.get(typeface.regular());
            Font spanItalicFont = fontManager.get(typeface.italic());

            long leading = PT.toSp(fontSize * 1.2f);
            long interParagraphSpacing = 0;
            long firstLineSpacing = PT.toSp(indentFirstLine ? fontSize * 2 : 0);

            // Set the distance between baselines based on the paragraph's main font.
            verticalList.setBaselineSkip(leading);

            if (newPage && !verticalList.getElements().isEmpty()) {
                verticalList.ejectPage();
            }

            if (marginTop != 0) {
                verticalList.addElement(new Box(0, marginTop, 0));
            }

            if (center) {
                horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
            }

            // Paragraph indent.
            if (firstLineSpacing != 0) {
                horizontalList.addElement(new Box(firstLineSpacing, 0, 0));
            }

            // Each span in the paragraph.
            for (Span span : block.getSpans()) {
                Font font = span.isItalic() ? spanItalicFont : spanRegularFont;

                String text = span.getText();
                if (allCaps) {
                    text = text.toUpperCase();
                }

                // Add the text to the current horizontal list.
                horizontalList.addText(text, font, fontSize, hyphenDictionary);
            }

            // Potentially add bookmark if we're starting a new part or chapter.
            switch (block.getBlockType()) {
                case BODY:
                default:
                    // Nothing special.
                    break;

                case PART_HEADER:
                    horizontalList.addElement(new SectionBookmark(block.getText()));
                    break;

                case CHAPTER_HEADER:
                    horizontalList.addElement(new SectionBookmark(block.getText()));
                    break;
            }

            // Eject the paragraph.
            horizontalList.addEndOfParagraph();

            // Break the horizontal list into HBox elements, adding them to the vertical list.
            horizontalList.format(verticalList, bodyWidth);

            if (ownPage) {
                verticalList.ejectPage();
                previousBlockType = null;
            } else {
                if (marginBottom != 0) {
                    verticalList.addElement(new Glue(marginBottom, marginBottom / 4, 0, false));
                }
                verticalList.addElement(new Glue(interParagraphSpacing, PT.toSp(3), 0, false));
                previousBlockType = block.getBlockType();
            }
        }

        // Eject the last page.
        verticalList.ejectPage();

        return verticalList;
    }

    /**
     * Adds the entire vertical list to the PDF, by first breaking it into pages and then adding the
     * pages to the PDF.
     */
    public void addVerticalListToPdf(VerticalList verticalList, BookLayout bookLayout, PDDocument pdDoc) throws IOException {
        // Format the vertical list into pages.
        List<Page> pages = verticalListToPages(verticalList, bookLayout.getBodyHeight());

        // Generate each page.
        addPagesToPdf(pages, bookLayout, pdDoc);
    }

    /**
     * Format the vertical list into a sequence of pages.
     * @param textHeight the max height of the text on a page.
     */
    public List<Page> verticalListToPages(VerticalList verticalList, long textHeight) {
        List<Page> pages = new ArrayList<>();

        verticalList.format(ElementSink.listSink(pages, Page.class), textHeight);

        return pages;
    }

    /**
     * Send each page (with the given size) to the PDF.
     */
    public void addPagesToPdf(List<Page> pages, BookLayout bookLayout, PDDocument pdDoc) throws IOException {
        for (Page page : pages) {
            addPageToPdf(page, bookLayout, pdDoc);
        }
    }

    /**
     * Add the VBox as a page to the PDF.
     */
    public void addPageToPdf(Page page, BookLayout bookLayout, PDDocument pdDoc) throws IOException {
        PDPage pdPage = new PDPage();
        pdDoc.addPage(pdPage);
        pdPage.setMediaBox(new PDRectangle(
                PT.fromSpAsFloat(bookLayout.getPageWidth()),
                PT.fromSpAsFloat(bookLayout.getPageHeight())));

        PDPageContentStream contents = new PDPageContentStream(pdDoc, pdPage);

        // Draw the margins for debugging.
        if (DRAW_MARGINS) {
            PdfUtil.drawDebugRectangle(contents,
                    bookLayout.getPageMargin(),
                    bookLayout.getPageMargin(),
                    bookLayout.getBodyWidth(),
                    bookLayout.getBodyHeight());
        }

        // Start at top of page.
        long y = bookLayout.getPageHeight() - bookLayout.getPageMargin();

        // Lay out each element in the page.
        for (Element element : page.getElements()) {
            long advanceY = element.layOutVertically(bookLayout.getPageMargin(), y, contents);

            y -= advanceY;
        }

        // Draw the page number.
        bookLayout.drawHeadline(page, contents);

        contents.close();
    }
}


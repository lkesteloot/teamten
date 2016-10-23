
package com.teamten.typeset;

import com.google.common.base.Stopwatch;
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Converts a document to a PDF.
 */
public class Typesetter {
    private static final boolean DRAW_MARGINS = false;
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
        FontManager fontManager = new PdfBoxFontManager(pdDoc);
        BookLayout bookLayout = new BookLayout();

        // Add document metadata.
        Config config = new Config();
        for (Map.Entry<String,String> entry : doc.getMetadata()) {
            Config.Key key = Config.Key.fromHeader(entry.getKey());
            config.add(key, entry.getValue());
        }

        // Load the hyphenation dictionary.
        String language = config.getString(Config.Key.LANGUAGE);
        if (language == null) {
            language = "en";
        }
        HyphenDictionary hyphenDictionary;
        try {
            hyphenDictionary = HyphenDictionary.fromResource(language);
        } catch (FileNotFoundException e) {
            System.out.printf("Warning: No hyphenation dictionary for language \"" + language + "\"\n");
            // Hyphenation won't happen.
            hyphenDictionary = null;
        }

        List<Page> pages = null;
        Bookmarks bookmarks = Bookmarks.empty();
        int pass;
        for (pass = 0; pass < MAX_ITERATIONS; pass++) {
            System.out.printf("Pass %d:\n", pass + 1);
            Stopwatch stopwatch = Stopwatch.createStarted();
            VerticalList verticalList = docToVerticalList(doc, config, bookLayout, fontManager, hyphenDictionary);
            System.out.println("  Horizontal layout: " + stopwatch);

            // Format the vertical list into pages.
            stopwatch = Stopwatch.createStarted();
            pages = verticalListToPages(verticalList, config.getBodyHeight());
            System.out.println("  Vertical layout: " + stopwatch);

            // Get the full list of bookmarks.
            Bookmarks newBookmarks = Bookmarks.fromPages(pages);
            if (newBookmarks.equals(bookmarks)) {
                // We've converged, we can stop.
                break;
            }

            // Try again with these new bookmarks.
            bookmarks = newBookmarks;

            // Figure out where the sections are.
            bookLayout.configureFromBookmarks(bookmarks);
        }
        if (pass == MAX_ITERATIONS) {
            throw new IllegalStateException("took too many passes to converge on stable page numbers");
        }

        // Send pages to PDF.
        Stopwatch stopwatch = Stopwatch.createStarted();
        addPagesToPdf(pages, config, bookLayout, fontManager, pdDoc);
        System.out.println("Adding to PDF: " + stopwatch);

        return pdDoc;
    }

    /**
     * Converts a DOM document to a vertical list.
     */
    private VerticalList docToVerticalList(Doc doc, Config config, BookLayout bookLayout,
                                           FontManager fontManager, HyphenDictionary hyphenDictionary) throws IOException {

        VerticalList verticalList = new VerticalList();

        BlockType previousBlockType = null;
        for (Block block : doc.getBlocks()) {
            Typeface typeface;
            double fontSize;
            boolean indentFirstLine = false;
            boolean allCaps = false;
            boolean center = false;
            boolean newPage = false;
            boolean oddPage = false;
            boolean ownPage = false;
            boolean addTracking = false;
            boolean smallCaps = false;
            long marginTop = 0;
            long marginBottom = 0;
            HorizontalList horizontalList = new HorizontalList();

            switch (block.getBlockType()) {
                case BODY:
                    typeface = Typeface.TIMES_NEW_ROMAN;
                    fontSize = 11;
                    indentFirstLine = previousBlockType == BlockType.BODY;
                    break;

                case PART_HEADER:
                    typeface = Typeface.TIMES_NEW_ROMAN;
                    fontSize = 19;
                    center = true;
                    marginTop = IN.toSp(1.75);
                    oddPage = true;
                    ownPage = true;
                    addTracking = true;
                    smallCaps = true;
                    break;

                case CHAPTER_HEADER:
                case MINOR_SECTION_HEADER:
                    typeface = Typeface.TIMES_NEW_ROMAN;
                    fontSize = 14;
                    center = true;
                    marginTop = IN.toSp(0.75);
                    marginBottom = IN.toSp(0.75);
                    oddPage = true;
                    addTracking = true;
                    smallCaps = true;
                    break;

                case HALF_TITLE_PAGE:
                    generateHalfTitlePage(config, bookLayout, verticalList, fontManager);
                    continue;

                case TITLE_PAGE:
                    generateTitlePage(config, bookLayout, verticalList, fontManager);
                    continue;

                case COPYRIGHT_PAGE:
                    generateCopyrightPage(config, bookLayout, verticalList, fontManager);
                    continue;

                case TABLE_OF_CONTENTS:
                    generateTableOfContents(config, bookLayout, verticalList, fontManager);
                    continue;

                default:
                    System.out.println("Warning: Unknown block type " + block.getBlockType());
                    continue;
            }

            Font spanRegularFont = fontManager.get(typeface, FontVariant.REGULAR);
            Font spanItalicFont = fontManager.get(typeface, FontVariant.ITALIC);
            Font spanSmallCapsFont = fontManager.get(typeface, FontVariant.SMALL_CAPS);

            if (smallCaps) {
                spanRegularFont = spanSmallCapsFont;
                // No small caps for italics.
            }
            if (addTracking) {
                spanRegularFont = new TrackingFont(spanRegularFont, 0.1, 0.5);
                spanItalicFont = new TrackingFont(spanItalicFont, 0.1, 0.5);
                spanSmallCapsFont = new TrackingFont(spanSmallCapsFont, 0.1, 0.5);
            }

            long leading = PT.toSp(fontSize * 1.2f);
            long interParagraphSpacing = 0;
            long firstLineSpacing = PT.toSp(indentFirstLine ? fontSize * 2 : 0);

            // Set the distance between baselines based on the paragraph's main font.
            verticalList.setBaselineSkip(leading);

            if (oddPage) {
                verticalList.oddPage();
            } else if (newPage) {
                verticalList.newPage();
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
                Font font = span.isSmallCaps() ? spanSmallCapsFont :
                        span.isItalic() ? spanItalicFont : spanRegularFont;

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
                    // Nothing special.
                    break;

                case PART_HEADER:
                    horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.PART, block.getText()));
                    break;

                case CHAPTER_HEADER:
                    horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.CHAPTER, block.getText()));
                    break;

                case MINOR_SECTION_HEADER:
                    horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.MINOR_SECTION, block.getText()));
                    break;

                default:
                    System.out.println("Warning: Unknown block type " + block.getBlockType());
                    break;
            }

            // Eject the paragraph.
            horizontalList.addEndOfParagraph();

            // Break the horizontal list into HBox elements, adding them to the vertical list.
            horizontalList.format(verticalList, config.getBodyWidth());

            if (ownPage) {
                verticalList.oddPage();
                // Space at the top of the section.
                verticalList.addElement(new Box(0, IN.toSp(2.0), 0));
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
    public void addVerticalListToPdf(VerticalList verticalList, Config config, BookLayout bookLayout,
                                     FontManager fontManager, PDDocument pdDoc) throws IOException {

        // Format the vertical list into pages.
        List<Page> pages = verticalListToPages(verticalList, config.getBodyHeight());

        // Generate each page.
        addPagesToPdf(pages, config, bookLayout, fontManager, pdDoc);
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
    public void addPagesToPdf(List<Page> pages, Config config, BookLayout bookLayout,
                              FontManager fontManager, PDDocument pdDoc) throws IOException {

        for (Page page : pages) {
            addPageToPdf(page, config, bookLayout, fontManager, pdDoc);
        }
    }

    /**
     * Add the VBox as a page to the PDF.
     */
    public void addPageToPdf(Page page, Config config, BookLayout bookLayout,
                             FontManager fontManager, PDDocument pdDoc) throws IOException {

        PDPage pdPage = new PDPage();
        pdDoc.addPage(pdPage);
        pdPage.setMediaBox(new PDRectangle(
                PT.fromSpAsFloat(config.getPageWidth()),
                PT.fromSpAsFloat(config.getPageHeight())));

        PDPageContentStream contents = new PDPageContentStream(pdDoc, pdPage);

        // Draw the margins for debugging.
        if (DRAW_MARGINS) {
            PdfUtil.drawDebugRectangle(contents,
                    config.getPageMargin(),
                    config.getPageMargin(),
                    config.getBodyWidth(),
                    config.getBodyHeight());
        }

        // Start at top of page.
        long y = config.getPageHeight() - config.getPageMargin();

        // Lay out each element in the page.
        for (Element element : page.getElements()) {
            long advanceY = element.layOutVertically(config.getPageMargin(), y, contents);

            y -= advanceY;
        }

        // Draw the page number.
        drawHeadline(page, config, bookLayout, fontManager, contents);

        contents.close();
    }

    /**
     * Adds the half-title page (the one with only the title on it) to the vertical list. Does not eject the page.
     */
    private void generateHalfTitlePage(Config config, BookLayout bookLayout, VerticalList verticalList,
                                       FontManager fontManager) throws IOException {

        String title = config.getString(Config.Key.TITLE);
        if (title == null) {
            return;
        }

        long marginTop = IN.toSp(2.0);
        // TODO: Get from book layout:
        Font titleFont = new TrackingFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.SMALL_CAPS), 0.1, 0.5);
        float titleFontSize = 19.0f;

        // Assume we're at the very beginning of the book, and we want an entire blank page at the front.
        verticalList.ejectPage();
        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Title.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(title, titleFont, titleFontSize, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.HALF_TITLE_PAGE, title));
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());
    }

    /**
     * Adds the title page to the vertical list. Does not eject the page.
     */
    private void generateTitlePage(Config config, BookLayout bookLayout, VerticalList verticalList,
                                   FontManager fontManager) throws IOException {

        String title = config.getString(Config.Key.TITLE);
        String author = config.getString(Config.Key.AUTHOR);
        String publisherName = config.getString(Config.Key.PUBLISHER_NAME);
        String publisherLocation = config.getString(Config.Key.PUBLISHER_LOCATION);
        if (title == null || author == null) {
            return;
        }

        long marginTop = IN.toSp(0.5);
        long titleMargin = IN.toSp(1.5);
        long publisherNameMargin = IN.toSp(4.0);
        long publisherLocationMargin = IN.toSp(0.02);
        // TODO: Get from book layout:
        Font authorFont = new TrackingFont(new SmallCapsFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR), 0.8f), 0.1, 0.5);
        float authorFontSize = 14.0f;
        Font titleFont = new TrackingFont(new SmallCapsFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR), 0.8f), 0.1, 0.5);
        float titleFontSize = 27.0f;
        Font publisherNameFont = new SmallCapsFont(new TrackingFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR), 0.1, 0.5), 0.8f);
        float publisherNameFontSize = 9.0f;
        Font publisherLocationFont = fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.ITALIC);
        float publisherLocationFontSize = 9.0f;

        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Author.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(author, authorFont, authorFontSize, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.TITLE_PAGE, title));
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        verticalList.addElement(new Box(0, titleMargin, 0));

        // Title.
        horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(title, titleFont, titleFontSize, null);
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        if (publisherName != null) {
            verticalList.addElement(new Box(0, publisherNameMargin, 0));

            // Publisher name.
            horizontalList = new HorizontalList();
            horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
            horizontalList.addText(publisherName, publisherNameFont, publisherNameFontSize, null);
            horizontalList.addEndOfParagraph();
            horizontalList.format(verticalList, config.getBodyWidth());

            if (publisherLocation != null) {
                verticalList.addElement(new Box(0, publisherLocationMargin, 0));

                // Publisher location.
                horizontalList = new HorizontalList();
                horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
                horizontalList.addText(publisherLocation, publisherLocationFont, publisherLocationFontSize, null);
                horizontalList.addEndOfParagraph();
                horizontalList.format(verticalList, config.getBodyWidth());
            }
        }
    }

    /**
     * Adds the copyright page to the vertical list. Does not eject the page.
     */
    private void generateCopyrightPage(Config config, BookLayout bookLayout, VerticalList verticalList,
                                       FontManager fontManager) throws IOException {

        String copyright = config.getString(Config.Key.COPYRIGHT);
        String printing = config.getString(Config.Key.PRINTING);

        if (copyright == null) {
            return;
        }

        long marginTop = IN.toSp(2.5);
        long printingMargin = IN.toSp(4.0);
        // TODO: Get from book layout:
        Font copyrightFont = fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.ITALIC);
        float copyrightFontSize = 11.0f;
        Font printingFont = fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.SMALL_CAPS);
        float printingFontSize = 9.0f;

        verticalList.newPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Copyright.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(copyright, copyrightFont, copyrightFontSize, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.COPYRIGHT_PAGE, copyright));
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        if (printing != null) {
            verticalList.addElement(new Box(0, printingMargin, 0));

            // Publisher location.
            horizontalList = new HorizontalList();
            horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
            horizontalList.addText(printing, printingFont, printingFontSize, null);
            horizontalList.addEndOfParagraph();
            horizontalList.format(verticalList, config.getBodyWidth());
        }
    }

    /**
     * Adds the table of contents to the vertical list. Does not eject the page.
     */
    private void generateTableOfContents(Config config, BookLayout bookLayout, VerticalList verticalList,
                                         FontManager fontManager) throws IOException {

        long marginTop = IN.toSp(1.0);
        long paddingBelowTitle = IN.toSp(0.75);

        String tocTitle = config.getString(Config.Key.TOC_TITLE);
        if (tocTitle == null) {
            tocTitle = "Table of Contents";
        }

        // TODO: Get from book layout:
        Font titleFont = new TrackingFont(fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.SMALL_CAPS), 0.1, 0.5);
        float titleFontSize = 14.0f;
        Font entryFont = fontManager.get(Typeface.TIMES_NEW_ROMAN, FontVariant.REGULAR);
        Font smallCapsEntryFont = new SmallCapsFont(entryFont, 0.8f);
        float entryFontSize = 11.0f;
        long boxWidth = IN.toSp(1.0);
        long boxHeight = PT.toSp(0.5);

        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Title.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(tocTitle, titleFont, titleFontSize, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.TABLE_OF_CONTENTS, tocTitle));
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        // Space between title and line.
        verticalList.addElement(new Glue(paddingBelowTitle/3, 0, 0, false));

        // Centered line.
        horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addElement(new Rule(boxWidth, boxHeight, 0));
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        // Space below line.
        verticalList.addElement(new Glue(paddingBelowTitle*2/3, 0, 0, false));

        long leading = PT.toSp(entryFontSize * 1.2f);
        verticalList.setBaselineSkip(leading);

        long previousMarginBelow = 0;
        long interEntryMargin = PT.toSp(entryFontSize*0.8);
        Leader leader = new Leader(entryFont, entryFontSize, " .   ", PT.toSp(1));

        // List each section.
        for (Map.Entry<Integer,SectionBookmark> entry : bookLayout.sections()) {
            int physicalPageNumber = entry.getKey();
            SectionBookmark sectionBookmark = entry.getValue();
            if (sectionBookmark.getType().isIncludedInTableOfContents()) {
                long indent = 0;
                // Above and below the entry. The actual space is the max of the two.
                long marginAbove = 0;
                long marginBelow = 0;
                String name = sectionBookmark.getName();
                String pageLabel = bookLayout.getPageNumberLabel(physicalPageNumber);
                Font sectionNameFont = entryFont;

                switch (sectionBookmark.getType()) {
                    case PART:
                        marginAbove = interEntryMargin;
                        marginBelow = interEntryMargin;
                        sectionNameFont = smallCapsEntryFont;
                        break;

                    case CHAPTER:
                        indent = PT.toSp(entryFontSize);
                        break;

                    case MINOR_SECTION:
                        marginAbove = interEntryMargin;
                        marginBelow = interEntryMargin;
                        break;

                    default:
                        System.out.println("Warning: Unknown section bookmark type " + sectionBookmark.getType());
                        break;
                }

                // Intra-entry margin.
                verticalList.addElement(new Glue(Math.max(previousMarginBelow, marginAbove), 0, 0, false));

                horizontalList = new HorizontalList();
                if (indent > 0) {
                    horizontalList.addElement(new Box(indent, 0, 0));
                }
                horizontalList.addText(name, sectionNameFont, entryFontSize, null);
                horizontalList.addElement(leader);
                horizontalList.addText(pageLabel, entryFont, entryFontSize, null);
                horizontalList.addElement(new Penalty(-Penalty.INFINITY));
                horizontalList.format(verticalList, config.getBodyWidth());

                previousMarginBelow = marginBelow;
            }
        }
    }

    /**
     * Draw the headline (page number of part or chapter title) at the top of the page.
     */
    public void drawHeadline(Page page, Config config, BookLayout bookLayout,
                             FontManager fontManager, PDPageContentStream contents) throws IOException {

        int physicalPageNumber = page.getPhysicalPageNumber();

        if (bookLayout.shouldDrawHeadline(physicalPageNumber)) {
            String pageNumberLabel = bookLayout.getPageNumberLabel(physicalPageNumber);
            String headlineLabel = bookLayout.getHeadlineLabel(physicalPageNumber, config);
            long pageMargin = config.getPageMargin();
            TypefaceVariantSize pageNumberFontDesc = config.getFont(Config.Key.PAGE_NUMBER_FONT);
            FontSize pageNumberFont = fontManager.get(pageNumberFontDesc);

            long y = config.getPageHeight() - pageMargin + PT.toSp(pageNumberFont.getSize()*2.5);

            // Draw page number.
            long x;
            if (physicalPageNumber % 2 == 0) {
                // Even page, number on the left.
                x = pageMargin;
            } else {
                // Odd page, number on the right.
                long labelWidth = pageNumberFont.getFont().getStringMetrics(pageNumberLabel, pageNumberFont.getSize()).getWidth();
                x = config.getPageWidth() - pageMargin - labelWidth;
            }

            // TODO this doesn't kern.
            pageNumberFont.getFont().draw(pageNumberLabel, pageNumberFont.getSize(), x, y, contents);

            // Draw headline label.
            if (headlineLabel != null) {
                // Draw this in upper case. TODO put this into a style.
                headlineLabel = headlineLabel.toUpperCase();

                // TODO this doesn't kern.
                long labelWidth = pageNumberFont.getFont().getStringMetrics(headlineLabel, pageNumberFont.getSize()).getWidth();
                x = pageMargin + (config.getBodyWidth() - labelWidth)/2;

                pageNumberFont.getFont().draw(headlineLabel, pageNumberFont.getSize(), x, y, contents);
            }
        }
    }
}



package com.teamten.typeset;

import com.google.common.base.Stopwatch;
import com.teamten.font.FontManager;
import com.teamten.font.FontSize;
import com.teamten.font.FontVariant;
import com.teamten.font.PdfBoxFontManager;
import com.teamten.font.TrackingFont;
import com.teamten.font.TypefaceVariantSize;
import com.teamten.hyphen.HyphenDictionary;
import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;
import com.teamten.markdown.Doc;
import com.teamten.markdown.IndexSpan;
import com.teamten.markdown.MarkdownParser;
import com.teamten.markdown.Span;
import com.teamten.markdown.TextSpan;
import com.teamten.typeset.element.Box;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.Leader;
import com.teamten.typeset.element.Page;
import com.teamten.typeset.element.Penalty;
import com.teamten.typeset.element.Rule;
import com.teamten.typeset.element.SectionBookmark;
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
    private static final boolean DRAW_PAGE_BOUNDARY = true;
    private static final int FAKE_INDEX_LENGTH = 0;
    /**
     * The maximum number of times that we'll typeset the document without it converging on a stable set
     * of page numbers.
     */
    private static final int MAX_ITERATIONS = 5;

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
        config.fillWithDefaults();
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

        // Do several passes at laying out the book until the page numbers stabilize.
        List<Page> pages = null;
        Bookmarks bookmarks = Bookmarks.empty();
        int pass;
        for (pass = 0; pass < MAX_ITERATIONS; pass++) {
            System.out.printf("Pass %d:\n", pass + 1);
            Stopwatch stopwatch = Stopwatch.createStarted();
            VerticalList verticalList = docToVerticalList(doc, config, bookLayout,
                    fontManager, bookmarks, hyphenDictionary);
            System.out.println("  Horizontal layout: " + stopwatch);

            // Format the vertical list into pages.
            stopwatch = Stopwatch.createStarted();
            pages = verticalListToPages(verticalList, config.getBodyHeight());
            System.out.println("  Vertical layout: " + stopwatch);

            // Get the full list of bookmarks.
            Bookmarks newBookmarks = Bookmarks.fromPages(pages);
            /// newBookmarks.println(System.out);
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
                                           FontManager fontManager, Bookmarks bookmarks,
                                           HyphenDictionary hyphenDictionary) throws IOException {

        VerticalList verticalList = new VerticalList();

        BlockType previousBlockType = null;
        for (Block block : doc.getBlocks()) {
            Config.Key regularFontKey;
            TypefaceVariantSize regularFontDesc;
            boolean indentFirstLine = false;
            boolean allCaps = false;
            boolean center = false;
            boolean newPage = false;
            boolean oddPage = false;
            boolean ownPage = false;
            boolean addTracking = false;
            long marginTop = 0;
            long marginBottom = 0;
            HorizontalList horizontalList = new HorizontalList();

            switch (block.getBlockType()) {
                case BODY:
                    regularFontKey = Config.Key.BODY_FONT;
                    indentFirstLine = previousBlockType == BlockType.BODY;
                    break;

                case PART_HEADER:
                    regularFontKey = Config.Key.PART_HEADER_FONT;
                    center = true;
                    marginTop = IN.toSp(1.75);
                    oddPage = true;
                    ownPage = true;
                    addTracking = true;
                    break;

                case CHAPTER_HEADER:
                case MINOR_SECTION_HEADER:
                    regularFontKey = Config.Key.CHAPTER_HEADER_FONT;
                    center = true;
                    marginTop = IN.toSp(0.75);
                    marginBottom = IN.toSp(0.75);
                    oddPage = true;
                    addTracking = true;
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

                case INDEX:
                    generateIndex(config, bookLayout, verticalList, fontManager, bookmarks);
                    continue;

                default:
                    System.out.println("Warning: Unknown block type " + block.getBlockType());
                    continue;
            }

            regularFontDesc = config.getFont(regularFontKey);

            FontSize spanRegularFont = fontManager.get(regularFontDesc);
            FontSize spanItalicFont = fontManager.get(regularFontDesc.withVariant(FontVariant.ITALIC));
            FontSize spanSmallCapsFont = fontManager.get(regularFontDesc.withVariant(FontVariant.SMALL_CAPS));
            double fontSize = regularFontDesc.getSize();

            if (addTracking) {
                spanRegularFont = TrackingFont.create(spanRegularFont, 0.1, 0.5);
                spanItalicFont = TrackingFont.create(spanItalicFont, 0.1, 0.5);
                spanSmallCapsFont = TrackingFont.create(spanSmallCapsFont, 0.1, 0.5);
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
                if (span instanceof TextSpan) {
                    // Span for text that's part of the paragraph.
                    TextSpan textSpan = (TextSpan) span;
                    FontSize font = textSpan.isSmallCaps() ? spanSmallCapsFont :
                            textSpan.isItalic() ? spanItalicFont : spanRegularFont;

                    String text = textSpan.getText();
                    if (allCaps) {
                        text = text.toUpperCase();
                    }

                    // Add the text to the current horizontal list.
                    horizontalList.addText(text, font, hyphenDictionary);
                } else if (span instanceof IndexSpan) {
                    // Span that creates an index entry.
                    IndexSpan indexSpan = (IndexSpan) span;

                    horizontalList.addElement(new IndexBookmark(indexSpan.getEntries()));
                }
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
        if (DRAW_PAGE_BOUNDARY) {
            long inset = PT.toSp(0.0);
            PdfUtil.drawDebugRectangle(contents, inset, inset,
                    config.getPageWidth() - 2*inset, config.getPageHeight() - 2*inset);
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
        FontSize titleFont = fontManager.get(config.getFont(Config.Key.HALF_TITLE_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);

        // Assume we're at the very beginning of the book, and we want an entire blank page at the front.
        verticalList.ejectPage();
        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Title.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(title, titleFont, null);
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
        FontSize authorFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_AUTHOR_FONT));
        authorFont = TrackingFont.create(authorFont, 0.1, 0.5);
        FontSize titleFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);
        FontSize publisherNameFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_PUBLISHER_NAME_FONT));
        publisherNameFont = TrackingFont.create(publisherNameFont, 0.1, 0.5);
        FontSize publisherLocationFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_PUBLISHER_LOCATION_FONT));

        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Author.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(author, authorFont, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.TITLE_PAGE, title));
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        verticalList.addElement(new Box(0, titleMargin, 0));

        // Title.
        horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(title, titleFont, null);
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        if (publisherName != null) {
            verticalList.addElement(new Box(0, publisherNameMargin, 0));

            // Publisher name.
            horizontalList = new HorizontalList();
            horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
            horizontalList.addText(publisherName, publisherNameFont, null);
            horizontalList.addEndOfParagraph();
            horizontalList.format(verticalList, config.getBodyWidth());

            if (publisherLocation != null) {
                verticalList.addElement(new Box(0, publisherLocationMargin, 0));

                // Publisher location.
                horizontalList = new HorizontalList();
                horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
                horizontalList.addText(publisherLocation, publisherLocationFont, null);
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
        FontSize copyrightFont = fontManager.get(config.getFont(Config.Key.COPYRIGHT_PAGE_COPYRIGHT_FONT));
        FontSize printingFont = fontManager.get(config.getFont(Config.Key.COPYRIGHT_PAGE_PRINTING_FONT));

        verticalList.newPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Copyright.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(copyright, copyrightFont, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.COPYRIGHT_PAGE, copyright));
        horizontalList.addEndOfParagraph();
        horizontalList.format(verticalList, config.getBodyWidth());

        if (printing != null) {
            verticalList.addElement(new Box(0, printingMargin, 0));

            // Publisher location.
            horizontalList = new HorizontalList();
            horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
            horizontalList.addText(printing, printingFont, null);
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

        FontSize titleFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);
        FontSize partFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_PART_FONT));
        FontSize chapterFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_CHAPTER_FONT));
        double entryFontSize = chapterFont.getSize();
        long boxWidth = IN.toSp(1.0);
        long boxHeight = PT.toSp(0.5);

        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Title.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(tocTitle, titleFont, null);
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
        Leader leader = new Leader(chapterFont, " .   ", PT.toSp(1));

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
                FontSize sectionNameFont = chapterFont;

                switch (sectionBookmark.getType()) {
                    case PART:
                        marginAbove = interEntryMargin;
                        marginBelow = interEntryMargin;
                        sectionNameFont = partFont;
                        break;

                    case CHAPTER:
                        indent = PT.toSp(entryFontSize);
                        break;

                    case MINOR_SECTION:
                        marginAbove = interEntryMargin;
                        marginBelow = interEntryMargin;
                        break;

                    case INDEX:
                        // Ignore.
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
                horizontalList.addText(name, sectionNameFont, null);
                horizontalList.addElement(leader);
                horizontalList.addText(pageLabel, chapterFont, null);
                horizontalList.addElement(new Penalty(-Penalty.INFINITY));
                horizontalList.format(verticalList, config.getBodyWidth());

                previousMarginBelow = marginBelow;
            }
        }
    }

    /**
     * Adds the index to the vertical list. Does not eject the page.
     */
    private void generateIndex(Config config, BookLayout bookLayout, VerticalList verticalList,
                               FontManager fontManager, Bookmarks bookmarks) throws IOException {

        long marginTop = IN.toSp(1.0);
        long paddingBelowTitle = IN.toSp(0.75);

        String indexTitle = config.getString(Config.Key.INDEX_TITLE);
        if (indexTitle == null) {
            indexTitle = "Index";
        }

        FontSize titleFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);
        FontSize entryFont = fontManager.get(config.getFont(Config.Key.BODY_FONT));
        long boxWidth = IN.toSp(1.0);
        long boxHeight = PT.toSp(0.5);

        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Title.
        HorizontalList horizontalList = new HorizontalList();
        horizontalList.addElement(new Glue(0, PT.toSp(1), true, 0, false, true));
        horizontalList.addText(indexTitle, titleFont, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.INDEX, indexTitle));
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

        // Generate the index entries.
        IndexEntries indexEntries = new IndexEntries();
        if (FAKE_INDEX_LENGTH > 0) {
            indexEntries.makeFakeIndex(FAKE_INDEX_LENGTH);
        } else {
            bookmarks.entries().stream()
                    .filter((entry) -> (entry.getValue() instanceof IndexBookmark))
                    .forEach((entry) -> {
                        int physicalPageNumber = entry.getKey();
                        IndexBookmark indexBookmark = (IndexBookmark) entry.getValue();
                        indexEntries.add(indexBookmark.getEntries(), physicalPageNumber);
                    });
        }

        int columnCount = 2;
        long columnMargin = config.getBodyWidth() / 10;
        long textWidth = (config.getBodyWidth() - columnMargin*(columnCount - 1)) / columnCount;

        // Generate the paragraphs.
        generateIndexEntries(indexEntries, bookLayout, verticalList, entryFont, textWidth, 0);

        if (false) {
            System.out.println("Index:");
            indexEntries.println(System.out, "    ");
        }
    }

    /**
     * Generate the paragraphs for this set of index entries and add them to the vertical list.
     *
     * @param depth the depth of the recursion, where 0 is for entries, 1 for sub-entries, etc.
     */
    private void generateIndexEntries(IndexEntries indexEntries, BookLayout bookLayout, VerticalList verticalList,
                                      FontSize font, long textWidth, int depth) throws IOException {
        // Space between sections.
        long sectionBreak = PT.toSp(6.0);

        // How much to indent each sub-section. TODO this isn't really an indent, it's a first-line indent. Modify
        // to indent every line.
        long indent = PT.toSp(15.0);

        // We want to put a space between sections, so keep track of the last section's category (first letter).
        int previousCategory = -1;

        for (IndexEntry indexEntry : indexEntries.getEntries()) {
            String text = indexEntry.getText();
            int firstCh = text.codePointAt(0);

            // Use '@' to represent non-alphabetic first letters.
            int category = Character.isAlphabetic(firstCh) ? Character.toLowerCase(firstCh) : '@';
            if (previousCategory != -1 && category != previousCategory && depth == 0) {
                // Insert a small break.
                verticalList.addElement(new Glue(sectionBreak, sectionBreak/2, 0, false));
            }

            // Add the item text.
            StringBuilder builder = new StringBuilder();
            builder.append(text);

            // And its page numbers.
            List<Integer> physicalPageNumbers = indexEntry.getPhysicalPageNumbers();
            for (int i = 0; i < physicalPageNumbers.size(); i++) {
                // Start with a range of a single page.
                int firstPage = physicalPageNumbers.get(i);
                int lastPage = firstPage;

                // Walk further to extend the range, if they're contiguous.
                while (i < physicalPageNumbers.size() - 1 &&
                        physicalPageNumbers.get(i + 1) == lastPage + 1) {

                    lastPage++;
                    i++;
                }

                // Append the page or a range of pages.
                builder.append(", ");
                builder.append(bookLayout.getPageNumberLabel(firstPage));
                if (lastPage > firstPage) {
                    builder.append('\u2013'); // En-dash.
                    builder.append(bookLayout.getPageNumberLabel(lastPage));
                }
            }

            builder.append('.');
            HorizontalList horizontalList = new HorizontalList();
            long totalIndent = indent*depth;
            if (totalIndent > 0) {
                horizontalList.addElement(new Box(totalIndent, 0, 0));
            }
            horizontalList.addText(builder.toString(), font);
            horizontalList.addEndOfParagraph();
            horizontalList.format(verticalList, textWidth);

            // Now do the children of this entry.
            generateIndexEntries(indexEntry.getSubEntries(), bookLayout, verticalList, font,
                    textWidth - totalIndent, depth + 1);

            // Kee track of the last category.
            previousCategory = category;
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
            FontSize pageNumberFont = fontManager.get(config.getFont(Config.Key.PAGE_NUMBER_FONT));
            FontSize headlineFont = fontManager.get(config.getFont(Config.Key.HEADLINE_FONT));

            long y = config.getPageHeight() - pageMargin + PT.toSp(pageNumberFont.getSize()*2.5);

            // Draw page number.
            long x;
            if (physicalPageNumber % 2 == 0) {
                // Even page, number on the left.
                x = pageMargin;
            } else {
                // Odd page, number on the right.
                long labelWidth = pageNumberFont.getStringMetrics(pageNumberLabel).getWidth();
                x = config.getPageWidth() - pageMargin - labelWidth;
            }
            // TODO this doesn't kern.
            pageNumberFont.draw(pageNumberLabel, x, y, contents);

            // Draw headline label.
            if (headlineLabel != null) {
                // Draw this in upper case. TODO put this into a style.
                headlineLabel = headlineLabel.toUpperCase();

                // TODO this doesn't kern.
                long labelWidth = headlineFont.getStringMetrics(headlineLabel).getWidth();
                x = pageMargin + (config.getBodyWidth() - labelWidth)/2;

                headlineFont.draw(headlineLabel, x, y, contents);
            }
        }
    }
}


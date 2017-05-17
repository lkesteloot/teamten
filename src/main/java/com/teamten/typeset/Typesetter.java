/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package com.teamten.typeset;

import com.google.common.base.Stopwatch;
import com.teamten.font.FontManager;
import com.teamten.font.FontPack;
import com.teamten.font.PdfBoxFontManager;
import com.teamten.font.SizedFont;
import com.teamten.font.TrackingFont;
import com.teamten.hyphen.HyphenDictionary;
import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;
import com.teamten.markdown.Doc;
import com.teamten.markdown.FootnoteSpan;
import com.teamten.markdown.ImageSpan;
import com.teamten.markdown.IndexSpan;
import com.teamten.markdown.LabelSpan;
import com.teamten.markdown.MarkdownParser;
import com.teamten.markdown.PageRefSpan;
import com.teamten.markdown.Span;
import com.teamten.markdown.TextSpan;
import com.teamten.typeset.element.Box;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Footnote;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.HBox;
import com.teamten.typeset.element.Image;
import com.teamten.typeset.element.LabelBookmark;
import com.teamten.typeset.element.Leader;
import com.teamten.typeset.element.Page;
import com.teamten.typeset.element.Penalty;
import com.teamten.typeset.element.Rule;
import com.teamten.typeset.element.SectionBookmark;
import com.teamten.typeset.element.Text;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.jetbrains.annotations.NotNull;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PC;
import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Converts a document to a PDF.
 */
public class Typesetter {
    private static final boolean DRAW_MARGINS = false;
    private static final boolean DRAW_PAGE_BOUNDARY = false;
    private static final int FAKE_INDEX_LENGTH = 0;
    /**
     * The maximum number of times that we'll typeset the document without it converging on a stable set
     * of page numbers.
     */
    private static final int MAX_ITERATIONS = 5;
    private static long mTimeSpentLoadingImages;
    private static int mUnknownLabelReferenceCount;

    public static void main(String[] args) throws IOException {
        mTimeSpentLoadingImages = 0;

        // Load and parse the Markdown file.
        Stopwatch stopwatch = Stopwatch.createStarted();
        InputStream inputStream = new FileInputStream(args[0]);
        MarkdownParser parser = new MarkdownParser();
        Doc doc = parser.parse(inputStream);

        // Typeset the document.
        Typesetter typesetter = new Typesetter();
        PDDocument pdDoc = typesetter.typeset(doc);

        // Save the PDF.
        pdDoc.save(args[1]);

        System.out.printf("Total time spent loading images: %,d ms%n", mTimeSpentLoadingImages);
        System.out.println("Total time: " + stopwatch);
    }

    public PDDocument typeset(Doc doc) throws IOException {
        PDDocument pdDoc = new PDDocument();
        FontManager fontManager = new PdfBoxFontManager(pdDoc);
        Sections sections = new Sections();

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

        // Get a locale for sorting.
        Collator collator;
        switch (language) {
            case "en":
                collator = Collator.getInstance(Locale.US);
                break;

            case "fr":
                collator = Collator.getInstance(Locale.FRENCH);
                break;

            default:
                throw new IllegalStateException("Unknown language \"" + language + "\" for collation");
        }


        // Do several passes at laying out the book until the page numbers stabilize.
        List<Page> pages = null;
        Bookmarks bookmarks = Bookmarks.empty();
        int pass;
        for (pass = 0; pass < MAX_ITERATIONS; pass++) {
            mUnknownLabelReferenceCount = 0;
            System.out.printf("Pass %d:%n", pass + 1);
            Stopwatch stopwatch = Stopwatch.createStarted();
            VerticalList verticalList = docToVerticalList(doc, pdDoc, config, sections,
                    fontManager, bookmarks, collator, hyphenDictionary);
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
            sections.configureFromBookmarks(bookmarks);
        }
        if (pass == MAX_ITERATIONS) {
            throw new IllegalStateException("took too many passes to converge on stable page numbers");
        }

        // Warn if there are still unknown label references.
        if (mUnknownLabelReferenceCount > 0) {
            System.out.println("Warning: " + mUnknownLabelReferenceCount + " unknown label references");
        }

        // Send pages to PDF.
        Stopwatch stopwatch = Stopwatch.createStarted();
        addPagesToPdf(pages, config, sections, fontManager, pdDoc);
        System.out.println("Adding to PDF: " + stopwatch);

        return pdDoc;
    }

    /**
     * Converts a DOM document to a vertical list.
     */
    private VerticalList docToVerticalList(Doc doc, PDDocument pdDoc, Config config, Sections sections,
                                           FontManager fontManager, Bookmarks bookmarks,
                                           Collator collator, HyphenDictionary hyphenDictionary) throws IOException {

        VerticalList verticalList = new VerticalList();
        int footnoteNumber = 1;

        BlockType previousBlockType = null;
        for (Block block : doc.getBlocks()) {
            // Generate special pages (table of contents, index, etc.).
            switch (block.getBlockType()) {
                case HALF_TITLE_PAGE:
                    generateHalfTitlePage(config, sections, verticalList, fontManager);
                    continue;

                case TITLE_PAGE:
                    generateTitlePage(config, sections, verticalList, fontManager);
                    continue;

                case COPYRIGHT_PAGE:
                    generateCopyrightPage(config, sections, verticalList, fontManager);
                    continue;

                case TABLE_OF_CONTENTS:
                    generateTableOfContents(config, sections, verticalList, fontManager);
                    continue;

                case INDEX:
                    generateIndex(config, sections, verticalList, fontManager, bookmarks, collator, hyphenDictionary);
                    continue;

                case SEPARATOR:
                    previousBlockType = block.getBlockType();
                    generateSeparator(config, verticalList, fontManager);
                    continue;

                case VERTICAL_SPACE:
                    generateVerticalSpace(verticalList);
                    continue;

                case NEW_PAGE:
                    verticalList.newPage();
                    continue;

                case ODD_PAGE:
                    verticalList.oddPage();
                    continue;

                default:
                    // Single paragraph.
                    break;
            }

            // Get the style for this paragraph given its block type.
            ParagraphStyle paragraphStyle = ParagraphStyle.forBlock(block, previousBlockType, config, fontManager);

            // Reset the footnote number at start of chapters.
            if (paragraphStyle.isResetFootnoteNumber()) {
                footnoteNumber = 1;
            }

            // Set the distance between baselines based on the paragraph's main font.
            verticalList.setBaselineSkip(paragraphStyle.getLeading());

            if (paragraphStyle.isOddPage()) {
                verticalList.oddPage();
            } else if (paragraphStyle.isNewPage()) {
                verticalList.newPage();
            }

            if (paragraphStyle.getMarginTop() != 0) {
                verticalList.addElement(new Box(0, paragraphStyle.getMarginTop(), 0));
            }

            // Create a horizontal list for this paragraph.
            HorizontalList horizontalList = makeHorizontalListFromBlock(block, paragraphStyle, pdDoc, config,
                    fontManager, bookmarks, sections, hyphenDictionary, footnoteNumber);

            // Bump footnote number by the number of footnotes in this horizontal list.
            footnoteNumber += horizontalList.getFootnoteCount();

            // Break the horizontal list into HBox elements, adding them to the vertical list.
            OutputShape outputShape = paragraphStyle.makeOutputShape(config.getBodyWidth());
            horizontalList.format(verticalList, outputShape);

            // Eject if we're supposed to be on our own page.
            if (paragraphStyle.isOwnPage()) {
                verticalList.oddPage();
                // Space at the top of the section.
                verticalList.addElement(new Box(0, IN.toSp(2.0), 0));
                previousBlockType = null;
            } else {
                // If the paragraph shouldn't have a break after it (e.g., a section title), prohibit
                // it by having an infinite penalty before the glue.
                if (paragraphStyle.preventBreak()) {
                    verticalList.addElement(new Penalty(Penalty.INFINITY));
                }

                // Add bottom margin.
                if (paragraphStyle.getMarginBottom() != 0) {
                    verticalList.addElement(new Glue(paragraphStyle.getMarginBottom(),
                            paragraphStyle.getMarginBottom()/4, 0, false));
                }
                // Some flexibility between paragraphs, unless this is after poetry lines.
                if (block.getBlockType() != BlockType.POETRY) {
                    verticalList.addElement(new Glue(0, PT.toSp(3), 0, false));
                }
                previousBlockType = block.getBlockType();
            }
        }

        // Eject the last page.
        verticalList.ejectPage();

        // At least one blank page after the last page.
        verticalList.ejectPage();

        return verticalList;
    }

    @NotNull
    public static HorizontalList makeHorizontalListFromBlock(Block block, ParagraphStyle paragraphStyle,
                                                              PDDocument pdDoc, Config config, FontManager fontManager,
                                                              Bookmarks bookmarks, Sections sections,
                                                              HyphenDictionary hyphenDictionary, int footnoteNumber) throws IOException {
        HorizontalList horizontalList;

        if (paragraphStyle.isCenter()) {
            // Headers, etc.
            horizontalList = HorizontalList.centered();
        } else if (paragraphStyle.isAllowLineBreaks()) {
            // Normal paragraph.
            horizontalList = new HorizontalList();
        } else {
            // Code.
            horizontalList = HorizontalList.noLineBreaks();
        }

        // Add the counter at the front of a numbered list paragraph.
        if (block.getBlockType() == BlockType.NUMBERED_LIST) {
            List<Element> elements = new ArrayList<>();
            elements.add(new Glue(0, PT.toSp(1.0), true, 0, false, true));
            elements.add(new Text(block.getCounter() + ". ", paragraphStyle.getFontPack().getRegularFont()));
            HBox hbox = HBox.ofWidth(elements, paragraphStyle.getParagraphIndent());
            horizontalList.addElement(hbox);
        }

        // Add bullet at the front of a bullet paragraph.
        if (block.getBlockType() == BlockType.BULLET_LIST) {
            List<Element> elements = new ArrayList<>();
            elements.add(new Glue(0, PT.toSp(1.0), true, 0, false, true));
            elements.add(new Text("– ", paragraphStyle.getFontPack().getRegularFont()));
            HBox hbox = HBox.ofWidth(elements, paragraphStyle.getParagraphIndent());
            horizontalList.addElement(hbox);
        }

        // Each span in the paragraph.
        for (Span span : block.getSpans()) {
            if (span instanceof TextSpan) {
                // Span for text that's part of the paragraph. Don't hyphenate if we're centered.
                horizontalList.addTextSpan((TextSpan) span, paragraphStyle.getFontPack(),
                        paragraphStyle.isCenter() ? null : hyphenDictionary);
            } else if (span instanceof IndexSpan) {
                // Span that creates an index entry.
                IndexSpan indexSpan = (IndexSpan) span;

                horizontalList.addElement(new IndexBookmark(indexSpan.getEntries()));
            } else if (span instanceof ImageSpan) {
                // Span to include an image, though not necessarily right here.
                ImageSpan imageSpan = (ImageSpan) span;

                Path imagePath = Paths.get(imageSpan.getPathname());
                long maxWidth = config.getBodyWidth();
                long maxHeight = config.getBodyHeight()*8/10;

                // Make sure we have a resized version. Lulu recommends 300 DPI, there's no point
                // in loading a larger image.
                imagePath = Image.preprocessImage(imagePath, maxWidth, maxHeight);

                // Load the image. We could cache the image so that we don't load it again on
                // subsequent passes, but currently we spend less than one second total loading all
                // images for all passes.
                Stopwatch stopwatch = Stopwatch.createStarted();
                horizontalList.addElement(Image.load(imagePath, maxWidth, maxHeight, imageSpan.getCaption(), config,
                        fontManager, bookmarks, sections, hyphenDictionary, pdDoc));
                mTimeSpentLoadingImages += stopwatch.elapsed(TimeUnit.MILLISECONDS);
            } else if (span instanceof FootnoteSpan) {
                // Span to put a footnote at the bottom of the page.
                FootnoteSpan footnoteSpan = (FootnoteSpan) span;

                // What the footnote mark looks like.
                String mark = String.valueOf(footnoteNumber);

                // Draw the footnote in-line.
                SizedFont footnoteFont = fontManager.get(config.getFont(Config.Key.FOOTNOTE_NUMBER_FONT));
                long footnoteShift = config.getDistance(Config.Key.FOOTNOTE_SHIFT);
                Text text = new Text(mark, footnoteFont);
                HBox hbox = new HBox(Collections.singletonList(text), footnoteShift);
                horizontalList.addElement(hbox);

                // Smaller version for the one in the footnote.
                footnoteFont = footnoteFont.withScaledSize(Footnote.FOOTNOTE_FONT_SCALE);
                text = new Text(mark, footnoteFont);
                hbox = new HBox(Collections.singletonList(text), (long) (footnoteShift*Footnote.FOOTNOTE_FONT_SCALE));

                // Add the footnote text so that it's later placed at the bottom of the page.
                horizontalList.addElement(Footnote.create(hbox, footnoteSpan.getBlock(), config,
                        fontManager, bookmarks, sections, hyphenDictionary));

                // Increment footnote number. This is only valid within this block. After the block is processed
                // we increment the global footnote number by the number of footnotes in this block.
                footnoteNumber++;
            } else if (span instanceof LabelSpan) {
                LabelSpan labelSpan = (LabelSpan) span;
                horizontalList.addElement(new LabelBookmark(labelSpan.getName()));
            } else if (span instanceof PageRefSpan) {
                // Convert a page reference to a page number.
                PageRefSpan pageRefSpan = (PageRefSpan) span;
                Integer physicalPageNumber = bookmarks.getPhysicalPageNumberForLabel(pageRefSpan.getName());
                String pageNumber;
                if (physicalPageNumber == null) {
                    pageNumber = "UNKNOWN";
                    mUnknownLabelReferenceCount++;
                } else {
                    pageNumber = sections.getPageNumberLabel(physicalPageNumber);
                }
                horizontalList.addTextSpan(new TextSpan(pageNumber, pageRefSpan.getFlags()),
                        paragraphStyle.getFontPack(), null);
            } else {
                throw new IllegalStateException("unknown span type " + span.getClass().getSimpleName());
            }
        }

        // Potentially add bookmark if we're starting a new part or chapter.
        switch (block.getBlockType()) {
            case BODY:
            case BLOCK_QUOTE:
            case MINOR_HEADER:
            case NUMBERED_LIST:
            case BULLET_LIST:
            case CODE:
            case OUTPUT:
            case INPUT:
            case POETRY:
            case SIGNATURE:
            case CAPTION:
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
                throw new IllegalStateException("Unknown block type " + block.getBlockType());
        }

        // Eject the paragraph.
        if (paragraphStyle.isCenter()) {
            horizontalList.addElement(new Penalty(-Penalty.INFINITY));
        } else {
            horizontalList.addEndOfParagraph();
        }

        return horizontalList;
    }

    /**
     * Adds the entire vertical list to the PDF, by first breaking it into pages and then adding the
     * pages to the PDF.
     */
    public void addVerticalListToPdf(VerticalList verticalList, Config config, Sections sections,
                                     FontManager fontManager, PDDocument pdDoc) throws IOException {

        // Format the vertical list into pages.
        List<Page> pages = verticalListToPages(verticalList, config.getBodyHeight());

        // Generate each page.
        addPagesToPdf(pages, config, sections, fontManager, pdDoc);
    }

    /**
     * Format the vertical list into a sequence of pages.
     *
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
    public void addPagesToPdf(List<Page> pages, Config config, Sections sections,
                              FontManager fontManager, PDDocument pdDoc) throws IOException {

        for (Page page : pages) {
            addPageToPdf(page, config, sections, fontManager, pdDoc);
        }
    }

    /**
     * Add the VBox as a page to the PDF.
     */
    public void addPageToPdf(Page page, Config config, Sections sections,
                             FontManager fontManager, PDDocument pdDoc) throws IOException {

        PDPage pdPage = new PDPage();
        pdDoc.addPage(pdPage);
        pdPage.setMediaBox(new PDRectangle(
                PT.fromSpAsFloat(config.getPageWidth()),
                PT.fromSpAsFloat(config.getPageHeight())));

        PDPageContentStream contents = new PDPageContentStream(pdDoc, pdPage);

        // Compute the left margin of the page, which depends on whether it's a left or right page.
        long leftMargin = page.isLeftPage() ? config.getPageMarginOuter() : config.getPageMarginInner();

        // Draw the margins for debugging.
        if (DRAW_MARGINS) {
            PdfUtil.drawDebugRectangle(contents,
                    config.getPageMarginBottom(),
                    leftMargin,
                    config.getBodyWidth(),
                    config.getBodyHeight());
        }
        if (DRAW_PAGE_BOUNDARY) {
            long inset = PT.toSp(0.0);
            PdfUtil.drawDebugRectangle(contents, inset, inset,
                    config.getPageWidth() - 2*inset, config.getPageHeight() - 2*inset);
        }

        // Start at top of page.
        long y = config.getPageHeight() - config.getPageMarginTop();

        // Lay out each element in the page.
        for (Element element : page.getElements()) {
            long advanceY = element.layOutVertically(leftMargin, y, contents);

            y -= advanceY;
        }

        // Draw the page number.
        drawHeadline(page, config, sections, fontManager, contents);

        contents.close();
    }

    /**
     * Adds the half-title page (the one with only the title on it) to the vertical list. Does not eject the page.
     */
    private void generateHalfTitlePage(Config config, Sections sections, VerticalList verticalList,
                                       FontManager fontManager) throws IOException {

        String title = config.getString(Config.Key.TITLE);
        if (title == null) {
            return;
        }

        long marginTop = IN.toSp(2.0);
        SizedFont titleFont = fontManager.get(config.getFont(Config.Key.HALF_TITLE_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);

        // Assume we're at the very beginning of the book, and we want an entire blank page at the front.
        verticalList.ejectPage();
        verticalList.oddPage();
        verticalList.addElement(new Box(0, marginTop, 0));

        // Title.
        long leading = PT.toSp(titleFont.getSize()*1.2f);
        long oldLeading = verticalList.setBaselineSkip(leading);
        HorizontalList horizontalList = HorizontalList.centered();
        horizontalList.addText(title, titleFont, null);
        horizontalList.addElement(new SectionBookmark(SectionBookmark.Type.HALF_TITLE_PAGE, title));
        horizontalList.addElement(new Penalty(-Penalty.INFINITY));
        horizontalList.format(verticalList, config.getBodyWidth());
        verticalList.setBaselineSkip(oldLeading);
    }

    /**
     * Adds the title page to the vertical list. Does not eject the page.
     */
    private void generateTitlePage(Config config, Sections sections, VerticalList verticalList,
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
        long publisherMargin = IN.toSp(3.5);
        long publisherLocationMargin = IN.toSp(0.02);
        SizedFont authorFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_AUTHOR_FONT));
        authorFont = TrackingFont.create(authorFont, 0.1, 0.5);
        SizedFont titleFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);
        SizedFont publisherNameFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_PUBLISHER_NAME_FONT));
        publisherNameFont = TrackingFont.create(publisherNameFont, 0.1, 0.5);
        SizedFont publisherLocationFont = fontManager.get(config.getFont(Config.Key.TITLE_PAGE_PUBLISHER_LOCATION_FONT));

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
        long leading = PT.toSp(titleFont.getSize()*1.2f);
        long oldLeading = verticalList.setBaselineSkip(leading);
        horizontalList = HorizontalList.centered();
        horizontalList.addText(title, titleFont, null);
        horizontalList.addElement(new Penalty(-Penalty.INFINITY));
        horizontalList.format(verticalList, config.getBodyWidth());
        verticalList.setBaselineSkip(oldLeading);

        if (publisherName != null) {
            verticalList.addElement(new Box(0, publisherMargin, 0));

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
    private void generateCopyrightPage(Config config, Sections sections, VerticalList verticalList,
                                       FontManager fontManager) throws IOException {

        String copyright = config.getString(Config.Key.COPYRIGHT);
        String printing = config.getString(Config.Key.COLOPHON);

        if (copyright == null) {
            return;
        }

        long marginTop = IN.toSp(2.5);
        long printingMargin = IN.toSp(4.0);
        SizedFont copyrightFont = fontManager.get(config.getFont(Config.Key.COPYRIGHT_PAGE_COPYRIGHT_FONT));
        SizedFont printingFont = fontManager.get(config.getFont(Config.Key.COPYRIGHT_PAGE_COLOPHON_FONT));

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
    private void generateTableOfContents(Config config, Sections sections, VerticalList verticalList,
                                         FontManager fontManager) throws IOException {

        long marginTop = IN.toSp(0.6);
        long paddingBelowTitle = IN.toSp(0.75);
        boolean hasParts = sections.hasParts();

        String tocTitle = config.getString(Config.Key.TOC_TITLE);
        if (tocTitle == null) {
            tocTitle = "Table of Contents";
        }

        SizedFont titleFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);
        SizedFont partFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_PART_FONT));
        SizedFont chapterFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_CHAPTER_FONT));
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

        long leading = PT.toSp(entryFontSize*1.2f);
        verticalList.setBaselineSkip(leading);

        long previousMarginBelow = 0;
        long interEntryMargin = PT.toSp(entryFontSize*0.8);
        Leader leader = new Leader(chapterFont, " .   ", PT.toSp(1));

        // If we let each page number take as much space as it wants, then the dots of the leaders
        // won't line up on the right. This looks bad and real books don't do this. So we must guess
        // the widest page number and make them all take that much space.
        String widestPageNumber = "888";
        long widestPageNumberWidth = chapterFont.getStringMetrics(widestPageNumber).getWidth();
        // Add some padding.
        widestPageNumberWidth = 11*widestPageNumberWidth/10;

        // List each section.
        for (Map.Entry<Integer,SectionBookmark> entry : sections.sections()) {
            int physicalPageNumber = entry.getKey();
            SectionBookmark sectionBookmark = entry.getValue();
            if (sectionBookmark.getType().isIncludedInTableOfContents()) {
                long indent = 0;
                // Above and below the entry. The actual space is the max of the two.
                long marginAbove = 0;
                long marginBelow = 0;
                String name = sectionBookmark.getName();
                String pageLabel = sections.getPageNumberLabel(physicalPageNumber);
                SizedFont sectionNameFont = chapterFont;

                switch (sectionBookmark.getType()) {
                    case PART:
                        marginAbove = interEntryMargin;
                        marginBelow = interEntryMargin;
                        sectionNameFont = partFont;
                        break;

                    case CHAPTER:
                        if (hasParts) {
                            indent = PT.toSp(entryFontSize);
                        } else {
                            marginAbove = interEntryMargin;
                            marginBelow = interEntryMargin;
                        }
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
                horizontalList.addElement(HBox.rightAligned(new Text(pageLabel, chapterFont), widestPageNumberWidth));
                horizontalList.addElement(new Penalty(-Penalty.INFINITY));
                horizontalList.format(verticalList, config.getBodyWidth());

                previousMarginBelow = marginBelow;
            }
        }
    }

    /**
     * Adds the index to the vertical list. Does not eject the page.
     */
    private void generateIndex(Config config, Sections sections, VerticalList verticalList,
                               FontManager fontManager, Bookmarks bookmarks,
                               Collator collator, HyphenDictionary hyphenDictionary) throws IOException {

        long marginTop = IN.toSp(1.0);
        long paddingBelowTitle = IN.toSp(0.75);

        String indexTitle = config.getString(Config.Key.INDEX_TITLE);
        if (indexTitle == null) {
            indexTitle = "Index";
        }

        SizedFont titleFont = fontManager.get(config.getFont(Config.Key.TOC_PAGE_TITLE_FONT));
        titleFont = TrackingFont.create(titleFont, 0.1, 0.5);
        FontPack entryFontPack = FontPack.create(fontManager, config.getFont(Config.Key.BODY_FONT),
                config.getFont(Config.Key.BODY_CODE_FONT));
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
        IndexEntries indexEntries = new IndexEntries(collator);
        if (FAKE_INDEX_LENGTH > 0) {
            indexEntries.makeFakeIndex(collator, FAKE_INDEX_LENGTH);
        } else {
            bookmarks.entries().stream()
                    .filter((entry) -> (entry.getValue() instanceof IndexBookmark))
                    .forEach((entry) -> {
                        int physicalPageNumber = entry.getKey();
                        IndexBookmark indexBookmark = (IndexBookmark) entry.getValue();
                        indexEntries.add(indexBookmark.getEntries(), physicalPageNumber);
                    });
        }

        // Switch to two columns.
        ColumnLayout columnLayout = ColumnLayout.fromBodyWidth(2, config.getBodyWidth(), config.getBodyWidth()/10);
        verticalList.changeColumnLayout(columnLayout);

        // Generate the paragraphs.
        generateIndexEntries(indexEntries, sections, verticalList, entryFontPack, hyphenDictionary,
                columnLayout.getColumnWidth(), 0);

        // Switch back to a single column.
        verticalList.changeColumnLayout(ColumnLayout.single());
    }

    /**
     * Generate the paragraphs for this set of index entries and add them to the vertical list.
     *
     * @param depth the depth of the recursion, where 0 is for entries, 1 for sub-entries, etc.
     */
    private void generateIndexEntries(IndexEntries indexEntries, Sections sections, VerticalList verticalList,
                                      FontPack fontPack, HyphenDictionary hyphenDictionary,
                                      long textWidth, int depth) throws IOException {
        // Space between sections.
        long sectionBreak = PT.toSp(6.0);

        // How much to indent each sub-section.
        long indent = PT.toSp(15.0);
        long totalIndent = indent*depth;
        long hangingIndent = indent*2; // Not depth-dependent; matches Knuth index.

        // We want to put a space between sections, so keep track of the last section's category (first letter).
        int previousCategory = -1;

        // Process each index entry in order.
        for (IndexEntry indexEntry : indexEntries.getEntries()) {
            // See if we moved to a different category.
            int category = indexEntry.getCategory();
            if (previousCategory != -1 && category != previousCategory && depth == 0) {
                // Insert a small break.
                verticalList.addElement(new Glue(sectionBreak, sectionBreak/2, 0, false));
            }

            // The full text of the entry.
            Block block = indexEntry.getIndexParagraph(sections);

            // Build the horizontal list.
            HorizontalList horizontalList = HorizontalList.raggedRight();
            for (Span span : block.getSpans()) {
                if (span instanceof TextSpan) {
                    // Span for text that's part of the paragraph.
                    horizontalList.addTextSpan((TextSpan) span, fontPack, hyphenDictionary);
                } else {
                    throw new IllegalStateException("index spans must be text");
                }
            }
            horizontalList.addEndOfParagraph();
            OutputShape outputShape = OutputShape.singleLine(textWidth, totalIndent, hangingIndent);
            horizontalList.format(verticalList, outputShape);

            // Add a bit of stretchability right after an entry. Otherwise the only stretchability is between
            // sections, which may not happen on in a column. Match TeX's values.
            verticalList.addElement(new Glue(0, PT.toSp(0.8), 0, false));

            // Now do the children of this entry.
            generateIndexEntries(indexEntry.getSubEntries(), sections, verticalList, fontPack,
                    hyphenDictionary, textWidth, depth + 1);

            // Keep track of the last category so we can insert spaces.
            previousCategory = category;
        }
    }

    /**
     * Draw a separator.
     */
    private void generateSeparator(Config config, VerticalList verticalList, FontManager fontManager) {
        long verticalSpace = PC.toSp(1);
        long horizontalSpace = PC.toSp(1);
        Glue verticalGlue = new Glue(verticalSpace, verticalSpace, verticalSpace/2, false);
        Glue horizontalGlue = Glue.horizontal(horizontalSpace);

        // Space above.
        verticalList.addElement(verticalGlue);

        // Simple separator with three stars.
        SizedFont font = fontManager.get(config.getFont(Config.Key.BODY_FONT));
        HorizontalList horizontalList = HorizontalList.centered();
        horizontalList.addElement(new Text("*", font));
        horizontalList.addElement(horizontalGlue);
        horizontalList.addElement(new Text("*", font));
        horizontalList.addElement(horizontalGlue);
        horizontalList.addElement(new Text("*", font));
        horizontalList.addElement(new Penalty(-Penalty.INFINITY));

        OutputShape outputShape = OutputShape.fixed(config.getBodyWidth());
        horizontalList.format(verticalList, outputShape);

        // Space below.
        verticalList.addElement(verticalGlue);
    }

    /**
     * Leave some vertical space. This is not currently used and can be removed if it's getting in the way.
     */
    private void generateVerticalSpace(VerticalList verticalList) {
        long verticalSpace = PC.toSp(2);
        Glue verticalGlue = new Glue(verticalSpace, verticalSpace, verticalSpace/2, false);
        verticalList.addElement(verticalGlue);
    }

    /**
     * Draw the headline (page number of part or chapter title) at the top of the page.
     */
    public void drawHeadline(Page page, Config config, Sections sections,
                             FontManager fontManager, PDPageContentStream contents) throws IOException {

        int physicalPageNumber = page.getPhysicalPageNumber();

        if (sections.shouldDrawHeadline(physicalPageNumber)) {
            String pageNumberLabel = sections.getPageNumberLabel(physicalPageNumber);
            String headlineLabel = sections.getHeadlineLabel(physicalPageNumber, config);
            SizedFont pageNumberFont = fontManager.get(config.getFont(Config.Key.PAGE_NUMBER_FONT));
            SizedFont headlineFont = fontManager.get(config.getFont(Config.Key.HEADLINE_FONT));

            long y = config.getPageHeight() - config.getPageMarginTop() + PT.toSp(pageNumberFont.getSize()*2.5);

            // Draw page number.
            long x;
            if (page.isLeftPage()) {
                // Even page, number on the left.
                x = config.getPageMarginOuter();
            } else {
                // Odd page, number on the right.
                long labelWidth = pageNumberFont.getStringMetrics(pageNumberLabel).getWidth();
                x = config.getPageWidth() - config.getPageMarginOuter() - labelWidth;
            }
            // TODO this doesn't kern.
            pageNumberFont.draw(pageNumberLabel, x, y, contents);

            // Draw headline label.
            if (headlineLabel != null) {
                // Draw this in upper case.
                headlineLabel = headlineLabel.toUpperCase();

                // TODO this doesn't kern.
                // Center the text.
                long labelWidth = headlineFont.getStringMetrics(headlineLabel).getWidth();
                long leftMargin = page.isLeftPage() ? config.getPageMarginOuter() : config.getPageMarginInner();
                x = leftMargin + (config.getBodyWidth() - labelWidth)/2;

                headlineFont.draw(headlineLabel, x, y, contents);
            }
        }
    }
}


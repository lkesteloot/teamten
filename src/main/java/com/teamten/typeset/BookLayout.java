package com.teamten.typeset;

import com.teamten.util.RomanNumerals;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Manages the overall layout of the book. This includes the position and format of the page numbers,
 * the section headers, etc.
 */
public class BookLayout {
    /**
     * From physical page number to section bookmark.
     */
    private final NavigableMap<Integer,SectionBookmark> mSectionMap = new TreeMap<>();
    private final String mBookTitle;
    private final long mPageWidth;
    private final long mPageHeight;
    private final long mPageMargin;
    private final Font mPageNumberFont;
    private final float mPageNumberFontSize;
    private int mFirstFrontMatterPhysicalPage = 1;
    private int mFirstBodyMatterPhysicalPage = 5;

    public BookLayout(String bookTitle, long pageWidth, long pageHeight, long pageMargin, Font pageNumberFont, float pageNumberFontSize) {
        mBookTitle = bookTitle;
        mPageWidth = pageWidth;
        mPageHeight = pageHeight;
        mPageMargin = pageMargin;
        mPageNumberFont = pageNumberFont;
        mPageNumberFontSize = pageNumberFontSize;
    }

    public long getPageWidth() {
        return mPageWidth;
    }

    public long getPageHeight() {
        return mPageHeight;
    }

    public long getPageMargin() {
        return mPageMargin;
    }

    /**
     * The width of the text on the page.
     */
    public long getBodyWidth() {
        return mPageWidth - 2*mPageMargin;
    }

    /**
     * The height of the text on the page.
     */
    public long getBodyHeight() {
        return mPageHeight - 2*mPageMargin;
    }

    /**
     * Look through the bookmarks to figure out where the body starts and, for any given page, what
     * part and chapter it's in.
     */
    public void configureFromBookmarks(Bookmarks bookmarks) {
        mSectionMap.clear();

        mFirstFrontMatterPhysicalPage = 1;
        mFirstBodyMatterPhysicalPage = Integer.MAX_VALUE;

        // Go through each bookmark and capture all sections.
        bookmarks.forEach((entry) -> {
            Integer physicalPageNumber = entry.getKey();
            Bookmark bookmark = entry.getValue();

            if (bookmark instanceof SectionBookmark) {
                SectionBookmark sectionBookmark = (SectionBookmark) bookmark;

                // Here we guess where the body starts by looking at the first part. In a book without parts
                // and only chapters, we'd need a way to be told explicitly which chapter starts the body, perhaps
                // by having a different section type for front matter sections.
                if (sectionBookmark.getType() == SectionBookmark.Type.PART) {
                    if (physicalPageNumber < mFirstBodyMatterPhysicalPage) {
                        mFirstBodyMatterPhysicalPage = physicalPageNumber;
                    }
                }

                // Make sure that two sections don't start on the same page.
                if (mSectionMap.containsKey(physicalPageNumber)) {
                    // We can't display the logical page here because we've not figured out the
                    // final value of mFirstBodyMatterPhysicalPage.
                    System.out.printf("Warning: Duplicate sections for physical page %d (%s and %s).",
                            physicalPageNumber, mSectionMap.get(physicalPageNumber), sectionBookmark);
                } else {
                    mSectionMap.put(physicalPageNumber, sectionBookmark);
                }
            }
        });

        // Never found the body.
        if (mFirstBodyMatterPhysicalPage == Integer.MAX_VALUE) {
            mFirstBodyMatterPhysicalPage = 1;
            System.out.println("Warning: Never found the book's body.");
        }
    }

    /**
     * Returns an ordered sequence of the sections in this document. The integer key is the
     * physical page number.
     */
    public Iterable<Map.Entry<Integer,SectionBookmark>> sections() {
        return mSectionMap.entrySet();
    }

    /**
     * Draw the headline (page number of part or chapter title) at the top of the page.
     */
    public void drawHeadline(Page page, PDPageContentStream contents) throws IOException {
        int physicalPageNumber = page.getPhysicalPageNumber();

        if (shouldDrawHeadline(physicalPageNumber)) {
            String pageNumberLabel = getPageNumberLabel(physicalPageNumber);
            String headlineLabel = getHeadlineLabel(physicalPageNumber);

            // TODO pick nice vertical position:
            long y = mPageHeight - mPageMargin + PT.toSp(mPageNumberFontSize)*5/2;

            // Draw page number.
            long x;
            if (physicalPageNumber % 2 == 0) {
                // Even page, number on the left.
                x = mPageMargin;
            } else {
                // Odd page, number on the right.
                long labelWidth = mPageNumberFont.getStringMetrics(pageNumberLabel, mPageNumberFontSize).getWidth();
                x = mPageWidth - mPageMargin - labelWidth;
            }

            contents.beginText();
            contents.setFont(((PdfBoxFont) mPageNumberFont).getPdFont(), mPageNumberFontSize);
            contents.newLineAtOffset(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y));
            contents.showText(pageNumberLabel);
            contents.endText();

            // Draw headline label.
            if (headlineLabel != null) {
                // Draw this in upper case. TODO put this into a style.
                headlineLabel = headlineLabel.toUpperCase();

                long labelWidth = mPageNumberFont.getStringMetrics(headlineLabel, mPageNumberFontSize).getWidth();
                x = mPageMargin + (getBodyWidth() - labelWidth)/2;

                contents.beginText();
                contents.setFont(((PdfBoxFont) mPageNumberFont).getPdFont(), mPageNumberFontSize);
                contents.newLineAtOffset(PT.fromSpAsFloat(x), PT.fromSpAsFloat(y));
                contents.showText(headlineLabel);
                contents.endText();
            }
        }
    }

    /**
     * Whether we should draw a headline on this page.
     */
    private boolean shouldDrawHeadline(int physicalPageNumber) {
        // Draw on all pages except where sections start.
        return !mSectionMap.containsKey(physicalPageNumber);
    }

    /**
     * Returns the displayed page number label for the physical page number. The displayed page
     * number might be in roman or arabic numerals.
     */
    public String getPageNumberLabel(int physicalPageNumber) {
        if (physicalPageNumber >= mFirstBodyMatterPhysicalPage) {
            return String.valueOf(physicalPageNumber - mFirstBodyMatterPhysicalPage + 1);
        } else {
            return RomanNumerals.toString(physicalPageNumber - mFirstFrontMatterPhysicalPage + 1);
        }
    }

    /**
     * Return the string to display at the top center of the page. This is usually based on the current
     * part or chapter title.
     */
    private String getHeadlineLabel(int physicalPageNumber) {
        String headlineLabel = mBookTitle;

        if (physicalPageNumber % 2 == 1) {
            // Use section name on right-hand (odd) pages.
            Map.Entry<Integer, SectionBookmark> entry = mSectionMap.floorEntry(physicalPageNumber);
            if (entry != null) {
                SectionBookmark bookmark = entry.getValue();
                headlineLabel = bookmark.getName();
            }
        }

        return headlineLabel;
    }
}

package com.teamten.typeset;

import com.teamten.util.RomanNumerals;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Manages the overall layout of the book. This includes the position and format of the page numbers,
 * the section headers, etc.
 */
public class BookLayout {
    /**
     * From physical page number to section bookmark.
     */
    private final NavigableMap<Integer,SectionBookmark> mPageToSectionMap = new TreeMap<>();
    private final Map<SectionBookmark.Type,Integer> mSectionToPageMap = new HashMap<>();
    private final Map<MetadataKey,String> mMetadata = new HashMap<>();
    private final long mPageWidth;
    private final long mPageHeight;
    private final long mPageMargin;
    private final Font mPageNumberFont;
    private final float mPageNumberFontSize;
    private int mFirstFrontMatterPhysicalPage = 1;
    private int mFirstBodyMatterPhysicalPage = 5;

    public enum MetadataKey {
        TITLE,
        AUTHOR,
        PUBLISHER_NAME,
        PUBLISHER_LOCATION,
        COPYRIGHT,
        PRINTING,
        TOC_TITLE,
    }

    public BookLayout(long pageWidth, long pageHeight, long pageMargin, Font pageNumberFont, float pageNumberFontSize) {
        mPageWidth = pageWidth;
        mPageHeight = pageHeight;
        mPageMargin = pageMargin;
        mPageNumberFont = pageNumberFont;
        mPageNumberFontSize = pageNumberFontSize;
    }

    public void setMetadata(MetadataKey key, String value) {
        mMetadata.put(key, value);
    }

    public String getMetadata(MetadataKey key) {
        return mMetadata.get(key);
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
        mPageToSectionMap.clear();
        mSectionToPageMap.clear();

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
                if (mPageToSectionMap.containsKey(physicalPageNumber)) {
                    // We can't display the logical page here because we've not figured out the
                    // final value of mFirstBodyMatterPhysicalPage.
                    System.out.printf("Warning: Duplicate sections for physical page %d (%s and %s).",
                            physicalPageNumber, mPageToSectionMap.get(physicalPageNumber), sectionBookmark);
                } else {
                    mPageToSectionMap.put(physicalPageNumber, sectionBookmark);
                    mSectionToPageMap.put(sectionBookmark.getType(), physicalPageNumber);
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
        return mPageToSectionMap.entrySet();
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
            long y = mPageHeight - mPageMargin + PT.toSp(mPageNumberFontSize*2.5);

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

            // TODO this doesn't kern.
            mPageNumberFont.draw(pageNumberLabel, mPageNumberFontSize, x, y, contents);

            // Draw headline label.
            if (headlineLabel != null) {
                // Draw this in upper case. TODO put this into a style.
                headlineLabel = headlineLabel.toUpperCase();

                // TODO this doesn't kern.
                long labelWidth = mPageNumberFont.getStringMetrics(headlineLabel, mPageNumberFontSize).getWidth();
                x = mPageMargin + (getBodyWidth() - labelWidth)/2;

                mPageNumberFont.draw(headlineLabel, mPageNumberFontSize, x, y, contents);
            }
        }
    }

    /**
     * Whether we should draw a headline on this page.
     */
    private boolean shouldDrawHeadline(int physicalPageNumber) {
        // Don't draw on pages where sections start.
        if (mPageToSectionMap.containsKey(physicalPageNumber)) {
            return false;
        }

        // Don't draw on pages before the table of contents.
        Integer tocPage = mSectionToPageMap.get(SectionBookmark.Type.TABLE_OF_CONTENTS);
        if (tocPage != null && physicalPageNumber < tocPage) {
            return false;
        }

        // Draw on all the rest.
        return true;
    }

    /**
     * Returns the displayed page number label for the physical page number. The displayed page
     * number might be in roman or arabic numerals.
     */
    public String getPageNumberLabel(int physicalPageNumber) {
        if (physicalPageNumber >= mFirstBodyMatterPhysicalPage) {
            // Arabic numerals for body of book.
            return String.valueOf(physicalPageNumber - mFirstBodyMatterPhysicalPage + 1);
        } else {
            // Roman numerals for front matter.
            return RomanNumerals.toString(physicalPageNumber - mFirstFrontMatterPhysicalPage + 1);
        }
    }

    /**
     * Return the string to display at the top center of the page. This is usually based on the current
     * part or chapter title.
     */
    private String getHeadlineLabel(int physicalPageNumber) {
        String headlineLabel = getMetadata(MetadataKey.TITLE);

        if (physicalPageNumber % 2 == 1) {
            // Use section name on right-hand (odd) pages.
            Map.Entry<Integer, SectionBookmark> entry = mPageToSectionMap.floorEntry(physicalPageNumber);
            if (entry != null) {
                SectionBookmark bookmark = entry.getValue();
                headlineLabel = bookmark.getName();
            }
        }

        return headlineLabel;
    }
}

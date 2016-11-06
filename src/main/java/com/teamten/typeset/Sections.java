package com.teamten.typeset;

import com.teamten.typeset.element.Bookmark;
import com.teamten.typeset.element.SectionBookmark;
import com.teamten.util.RomanNumerals;

import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Keeps track of the positions of the sections (parts, chapters) of the book.
 */
public class Sections {
    /**
     * From physical page number to section bookmark.
     */
    private final NavigableMap<Integer,SectionBookmark> mPageToSectionMap = new TreeMap<>();
    private final Map<SectionBookmark.Type,Integer> mSectionToPageMap = new HashMap<>();
    private int mFirstFrontMatterPhysicalPage = 1;
    private int mFirstBodyMatterPhysicalPage = 1;

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
        bookmarks.entries().forEach((entry) -> {
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
     * Whether we should draw a headline on this page.
     */
    public boolean shouldDrawHeadline(int physicalPageNumber) {
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
    public String getHeadlineLabel(int physicalPageNumber, Config config) {
        String headlineLabel = config.getString(Config.Key.TITLE);
        if (headlineLabel == null) {
            headlineLabel = "";
        }

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

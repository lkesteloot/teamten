package com.teamten.typeset;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.io.PrintStream;
import java.util.List;

/**
 * Keeps track of a set of bookmarks and which page they're on.
 */
public class Bookmarks {
    private final SetMultimap<PageNumber,Bookmark> mPageNumberToBookmark = HashMultimap.create();

    private Bookmarks() {
        // Private constructor.
    }

    public static Bookmarks empty() {
        return new Bookmarks();
    }

    public static Bookmarks fromPages(List<VBox> pages) {
        Bookmarks bookmarks = new Bookmarks();

        PageNumber pageNumber = new PageNumber(1, false);
        for (VBox page : pages) {
            PageNumber finalPageNumber = pageNumber;
            page.visit((element) -> {
                if (element instanceof Bookmark) {
                    bookmarks.add(finalPageNumber, (Bookmark) element);
                }
            });

            pageNumber = pageNumber.successor();
        }

        return bookmarks;
    }

    /**
     * Add the bookmark at the specified page number.
     */
    private void add(PageNumber pageNumber, Bookmark bookmark) {
        mPageNumberToBookmark.put(pageNumber, bookmark);
    }

    /**
     * Print all bookmarks, ordered by page number.
     */
    public void println(PrintStream stream) {
        stream.println("Bookmarks:");
        mPageNumberToBookmark.keySet().stream().sorted().forEach((pageNumber) -> {
            mPageNumberToBookmark.get(pageNumber).forEach((bookmark) -> {
                stream.printf("%4s: %s\n", pageNumber, bookmark);
            });
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bookmarks bookmarks = (Bookmarks) o;

        return mPageNumberToBookmark.equals(bookmarks.mPageNumberToBookmark);

    }
}

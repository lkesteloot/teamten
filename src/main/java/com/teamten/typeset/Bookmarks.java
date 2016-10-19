package com.teamten.typeset;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

import java.io.PrintStream;
import java.util.List;

/**
 * Keeps track of a set of bookmarks and which physical page they're on.
 */
public class Bookmarks {
    private final SetMultimap<Integer,Bookmark> mPhysicalPageNumberToBookmark = HashMultimap.create();

    private Bookmarks() {
        // Private constructor.
    }

    public static Bookmarks empty() {
        return new Bookmarks();
    }

    public static Bookmarks fromPages(List<Page> pages) {
        Bookmarks bookmarks = new Bookmarks();

        for (Page page : pages) {
            page.visit((element) -> {
                if (element instanceof Bookmark) {
                    bookmarks.add(page.getPhysicalPageNumber(), (Bookmark) element);
                }
            });
        }

        return bookmarks;
    }

    /**
     * Add the bookmark at the specified page number.
     */
    private void add(Integer physicalPageNumber, Bookmark bookmark) {
        mPhysicalPageNumberToBookmark.put(physicalPageNumber, bookmark);
    }

    /**
     * Print all bookmarks, ordered by page number.
     */
    public void println(PrintStream stream) {
        stream.println("Bookmarks:");
        mPhysicalPageNumberToBookmark.keySet().stream().sorted().forEach((pageNumber) -> {
            mPhysicalPageNumberToBookmark.get(pageNumber).forEach((bookmark) -> {
                stream.printf("%4s: %s\n", pageNumber, bookmark);
            });
        });
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Bookmarks bookmarks = (Bookmarks) o;

        return mPhysicalPageNumberToBookmark.equals(bookmarks.mPhysicalPageNumberToBookmark);

    }
}

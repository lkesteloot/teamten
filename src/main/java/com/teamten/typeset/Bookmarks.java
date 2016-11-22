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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import com.teamten.typeset.element.Bookmark;
import com.teamten.typeset.element.Page;

import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
     * Get a set of bookmark entries, where the key is the physical page number and the
     * value is the bookmark
     */
    public Set<Map.Entry<Integer,Bookmark>> entries() {
        return mPhysicalPageNumberToBookmark.entries();
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
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Bookmarks bookmarks = (Bookmarks) o;

        return mPhysicalPageNumberToBookmark.equals(bookmarks.mPhysicalPageNumberToBookmark);

    }
}

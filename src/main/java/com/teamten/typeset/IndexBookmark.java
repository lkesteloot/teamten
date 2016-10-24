package com.teamten.typeset;

import com.teamten.typeset.element.Bookmark;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a reference from an index entry.
 */
public class IndexBookmark extends Bookmark {
    private final @NotNull List<String> mEntries;

    public IndexBookmark(List<String> entries) {
        mEntries = entries;
    }

    public List<String> getEntries() {
        return mEntries;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IndexBookmark that = (IndexBookmark) o;

        return mEntries.equals(that.mEntries);

    }

    @Override
    public int hashCode() {
        return mEntries.hashCode();
    }

    @Override
    public String toString() {
        String entries = mEntries.stream().collect(Collectors.joining(", "));

        return "Index entry \"" + entries + "\"";
    }
}

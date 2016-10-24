package com.teamten.typeset;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores a single index entry, its physical pages, and its sub-entries.
 */
public class IndexEntry implements Comparable<IndexEntry> {
    private final String mText;
    private final Set<Integer> mPhysicalPageNumbers = new HashSet<>();
    private final IndexEntries mSubEntries = new IndexEntries();

    public IndexEntry(String text) {
        mText = text;
    }

    /**
     * Add the specified page number to our set.
     */
    public void addPage(int physicalPageNumber) {
        mPhysicalPageNumbers.add(physicalPageNumber);
    }

    /**
     * Add the information from the other index entry into this one.
     */
    public void mergeWith(IndexEntry other) {
        mPhysicalPageNumbers.addAll(other.mPhysicalPageNumbers);
        mSubEntries.mergeWith(other.mSubEntries);
    }

    /**
     * Get the text to display in the index.
     */
    public String getText() {
        return mText;
    }

    public Set<Integer> getPhysicalPageNumbers() {
        return mPhysicalPageNumbers;
    }

    public IndexEntries getSubEntries() {
        return mSubEntries;
    }

    /**
     * Print the entries to the stream.
     */
    public void println(PrintStream stream, String indent) {
        stream.print(indent);
        stream.print(mText);

        if (!mPhysicalPageNumbers.isEmpty()) {
            stream.print(mPhysicalPageNumbers.stream()
                    .sorted()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", ", ", ", "")));
        }

        stream.println(".");
        mSubEntries.println(stream, indent + "    ");
    }

    @Override
    public int compareTo(IndexEntry o) {
        return mText.compareTo(o.mText);
    }
}

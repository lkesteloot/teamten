package com.teamten.typeset;

import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stores a set of index entries.
 */
public class IndexEntries {
    private final Map<String,IndexEntry> mEntryMap = new HashMap<>();

    /**
     * Adds an entry to the set.
     */
    public void add(IndexEntry indexEntry) {
        String text = indexEntry.getText();

        // See if we already have an entry for this text.
        IndexEntry existingEntry = mEntryMap.get(text);
        if (existingEntry == null) {
            // New entry, just add it.
            mEntryMap.put(text, indexEntry);
        } else {
            // We have an entry, we have to merge them.
            existingEntry.mergeWith(indexEntry);
        }
    }

    /**
     * Add an entry by a list of text (entry, sub-entry, etc.) and page number.
     */
    public void add(List<String> texts, int physicalPageNumber) {
        if (texts.isEmpty()) {
            throw new IllegalArgumentException("texts should not be empty");
        }

        // Add it to our map if it's not there.
        IndexEntry indexEntry = getOrCreate(texts.get(0));

        // If we're reached the leaf, add the page number. Otherwise recurse to the sub-entry.
        if (texts.size() == 1) {
            indexEntry.addPage(physicalPageNumber);
        } else {
            List<String> subEntries = texts.subList(1, texts.size());
            indexEntry.getSubEntries().add(subEntries, physicalPageNumber);
        }
    }

    /**
     * Adds a set of entries to our own.
     */
    public void mergeWith(IndexEntries other) {
        other.mEntryMap.values().forEach(this::add);
    }

    /**
     * Print the entries to the stream.
     */
    public void println(PrintStream stream, String indent) {
        mEntryMap.values().stream()
                .sorted()
                .forEach((indexEntry) -> {
                    indexEntry.println(stream, indent);
                });
    }

    /**
     * Get an existing entry or create one.
     */
    private IndexEntry getOrCreate(String text) {
        IndexEntry indexEntry = mEntryMap.get(text);
        if (indexEntry == null) {
            indexEntry = new IndexEntry(text);
            mEntryMap.put(text, indexEntry);
        }

        return indexEntry;
    }
}

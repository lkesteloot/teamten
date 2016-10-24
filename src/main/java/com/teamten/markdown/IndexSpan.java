package com.teamten.markdown;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stores an index reference.
 */
public class IndexSpan extends Span {
    private final List<String> mEntries;

    public IndexSpan(List<String> entries) {
        mEntries = entries;
    }

    /**
     * Create an index span from a single string, where the entries are separated by bars (|).
     */
    public IndexSpan(String entries) {
        this(Arrays.stream(entries.split("\\|")).map(String::trim).collect(Collectors.toList()));
    }

    /**
     * Return the entries, where the first entry is the primary one, the second is the subentry, etc.
     */
    public List<String> getEntries() {
        return mEntries;
    }
}

package com.teamten.typeset;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collector;
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

    /**
     * Get the full paragraph to display for this entry in the index.
     */
    public String getIndexParagraph(BookLayout bookLayout) {
        StringBuilder builder = new StringBuilder();

        // Add the index text.
        builder.append(mText);

        // And its page numbers.
        List<Integer> physicalPageNumbers = getPhysicalPageNumbers();
        for (int i = 0; i < physicalPageNumbers.size(); i++) {
            // Start with a range of a single page.
            int firstPage = physicalPageNumbers.get(i);
            int lastPage = firstPage;

            // Walk further to extend the range, if they're contiguous.
            while (i < physicalPageNumbers.size() - 1 && physicalPageNumbers.get(i + 1) == lastPage + 1) {
                lastPage++;
                i++;
            }

            // Append the page or a range of pages.
            builder.append(", ");
            builder.append(bookLayout.getPageNumberLabel(firstPage));
            if (lastPage > firstPage) {
                builder.append('\u2013'); // En-dash.
                builder.append(bookLayout.getPageNumberLabel(lastPage));
            }
        }

        builder.append('.');

        return builder.toString();
    }

    /**
     * Get the category for this index. This is based on the first letter of the text, so
     * "Alpha" and "Beta" are in different categories, but "Alpha" and "alpha" are the same.
     * All non-alphanumeric characters are in the same category. Returned values are
     * non-negative.
     */
    public int getCategory() {
        // Based on the first character.
        int firstCh = mText.codePointAt(0);

        // Use '@' to represent non-alphabetic first letters.
        return Character.isAlphabetic(firstCh) ? Character.toLowerCase(firstCh) : '@';
    }

    /**
     * Return a sorted list of page numbers for this entry.
     */
    public List<Integer> getPhysicalPageNumbers() {
        return mPhysicalPageNumbers.stream().sorted().collect(Collectors.toList());
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
        int ch1 = mText.codePointAt(0);
        int ch2 = o.mText.codePointAt(0);

        // If one is not alphabetic, put it first.
        if (Character.isAlphabetic(ch1) != Character.isAlphabetic(ch2)) {
            return Character.isAlphabetic(ch1) ? 1 : -1;
        }

        // Else compare case-insensitively.
        return mText.compareToIgnoreCase(o.mText);
    }
}

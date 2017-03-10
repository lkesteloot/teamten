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

import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores a single index entry, its physical pages, and its sub-entries.
 */
public class IndexEntry implements Comparable<IndexEntry> {
    private final Block mText;
    private final String mTextAsString;
    private final Set<Integer> mPhysicalPageNumbers = new HashSet<>();
    private final IndexEntries mSubEntries = new IndexEntries();

    public IndexEntry(Block text) {
        mText = text;
        mTextAsString = text.toBriefString();
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
    public Block getText() {
        return mText;
    }

    /**
     * Get the full paragraph to display for this entry in the index.
     */
    public Block getIndexParagraph(Sections sections) {
        Block.Builder builder = new Block.Builder(BlockType.BODY, 0);

        // Add the index text.
        builder.addBlock(mText);

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
            builder.addText(", ");
            builder.addText(sections.getPageNumberLabel(firstPage));
            if (lastPage > firstPage) {
                builder.addText("\u2013"); // En-dash.
                builder.addText(sections.getPageNumberLabel(lastPage));
            }
        }

        builder.addText(".");

        return builder.build();
    }

    /**
     * Get the category for this index. This is based on the first letter of the text, so
     * "Alpha" and "Beta" are in different categories, but "Alpha" and "alpha" are the same.
     * All non-alphanumeric characters are in the same category. Returned values are
     * non-negative.
     */
    public int getCategory() {
        // Based on the first character.
        int firstCh = mTextAsString.codePointAt(0);

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
        int ch1 = mTextAsString.codePointAt(0);
        int ch2 = o.mTextAsString.codePointAt(0);

        // If one is not alphabetic, put it first.
        if (Character.isAlphabetic(ch1) != Character.isAlphabetic(ch2)) {
            return Character.isAlphabetic(ch1) ? 1 : -1;
        }

        // Else compare case-insensitively.
        return mTextAsString.compareToIgnoreCase(o.mTextAsString);
    }
}

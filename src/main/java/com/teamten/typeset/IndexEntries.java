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

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

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
     * Return a sorted list of index entries.
     */
    public List<IndexEntry> getEntries() {
        return mEntryMap.values().stream().sorted().collect(Collectors.toList());
    }

    /**
     * Fill this object with count entries or sub-entries.
     */
    public void makeFakeIndex(int count) throws IOException {
        // Load a fake dictionary.
        Path dictionaryPath = Paths.get("/usr/share/dict/web2");
        List<String> words = Files.readAllLines(dictionaryPath);
        Random random = new Random(0);
        addFakeEntries(words, random, count, 0);
    }

    /**
     * Add count entries to the index.
     * @param words list of words to choose from.
     * @param random the random number generator to use.
     * @param count number of entries to add.
     * @param depth the depth of the index, where 0 is entries and 1 is subentries.
     */
    private void addFakeEntries(List<String> words, Random random, int count, int depth) {
        // Keep going until we have enough entries.
        while (count > 0) {
            // Add an entry with a random word.
            String word = words.get(random.nextInt(words.size()));
            switch (random.nextInt(30)) {
                case 0: word = "@lkesteloot"; break;
                case 1: word = "|whatever"; break;
                case 2: word = "&tc"; break;
                case 3: word = "1914-1918"; break;
            }
            IndexEntry indexEntry = new IndexEntry(word);
            add(indexEntry);
            count--;

            // Add random number of page references.
            int pageCount = random.nextInt(8) + 1;
            for (int i = 0; i < pageCount; i++) {
                indexEntry.addPage(random.nextInt(200) + 1);
            }

            // Occasionally add sub-entries.
            if (depth == 0 && random.nextInt(5) == 0) {
                int subEntryCount = Math.min(random.nextInt(8) + 1, count);
                if (subEntryCount > 0) {
                    indexEntry.getSubEntries().addFakeEntries(words, random, subEntryCount, depth + 1);
                    count -= subEntryCount;
                }
            }
        }
    }

    /**
     * Print the entries to the stream.
     */
    public void println(PrintStream stream, String indent) {
        getEntries().forEach((indexEntry) -> {
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

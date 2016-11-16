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

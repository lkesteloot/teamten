
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes a whole document, namely the header and the blocks.
 */
public class Doc {
    private final List<Block> mBlocks = new ArrayList<>();
    private final Map<String,String> mMetadata = new HashMap<>();

    /**
     * Add a block (paragraph) to this document.
     */
    public void addBlock(Block block) {
        mBlocks.add(block);
    }

    /**
     * Return an iterable of all the blocks in this document.
     */
    public Iterable<Block> getBlocks() {
        return mBlocks;
    }

    /**
     * Add a key/value pair to the document's metadata.
     */
    public void addMetadata(String key, String value) {
        mMetadata.put(key, value);
    }

    /**
     * Return an unordered iterable over the document's metadata.
     */
    public Iterable<Map.Entry<String,String>> getMetadata() {
        return mMetadata.entrySet();
    }
}


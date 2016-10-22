
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



package com.teamten.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a whole document, namely the header and the blocks.
 */
public class Doc {
    private final List<Block> mBlocks = new ArrayList<>();

    public void addBlock(Block block) {
        mBlocks.add(block);
    }

    public List<Block> getBlocks() {
        return mBlocks;
    }
}


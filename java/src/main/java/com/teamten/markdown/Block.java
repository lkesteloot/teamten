
package com.teamten.markdown;

import java.util.ArrayList;
import java.util.List;

/**
 * A block is a paragraph (similar to a block in HTML DOM). It's a sequence of spans.
 */
public class Block {
    private final BlockType mBlockType;
    private final List<Span> mSpans = new ArrayList<>();

    public Block(BlockType blockType) {
        mBlockType = blockType;
    }

    public BlockType getBlockType() {
        return mBlockType;
    }

    public void addSpan(Span span) {
        mSpans.add(span);
    }

    @Override // Object
    public String toString() {
        if (mSpans.isEmpty()) {
            return "No spans";
        } else {
            String first = mSpans.get(0).getText();
            return String.format("%d spans, starting with: %s ...", mSpans.size(),
                    first.substring(0, Math.min(1000, first.length())));
        }
    }
}

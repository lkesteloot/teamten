
package com.teamten.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

    public List<Span> getSpans() {
        return mSpans;
    }

    /**
     * Return a string version of all text spans.
     */
    public String getText() {
        return mSpans.stream().map(Span::getText).collect(Collectors.joining());
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
            return String.format("%s, %d spans, starting with: %s ...",
                    mBlockType, mSpans.size(),
                    first.substring(0, Math.min(1000, first.length())));
        }
    }

    /**
     * Builds a Block one character at a time.
     */
    public static class Builder {
        private final Block mBlock;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private boolean mIsItalic;
        private boolean mIsSmallCaps;

        public Builder(BlockType blockType) {
            mBlock = new Block(blockType);
        }

        /**
         * Add the character to the block.
         * @param ch the character to add.
         * @param isItalic whether the character should be displayed in italics.
         * @param isSmallCaps whether the character should be displayed in small caps.
         */
        public void add(char ch, boolean isItalic, boolean isSmallCaps) {
            if (isItalic != mIsItalic || isSmallCaps != mIsSmallCaps) {
                emitSpan();
                mIsItalic = isItalic;
                mIsSmallCaps = isSmallCaps;
            }

            mStringBuilder.append(ch);
        }

        /**
         * Returns whether any characters have been added so far.
         */
        public boolean isEmpty() {
            return mBlock.getSpans().isEmpty() && mStringBuilder.length() == 0;
        }

        /**
         * Builds the block and returns it. Do not call this more than once for a given builder.
         */
        public Block build() {
            emitSpan();
            return mBlock;
        }

        /**
         * Possibly emit span, if we have characters accumulated up.
         */
        private void emitSpan() {
            if (mStringBuilder.length() > 0) {
                Span span = new Span(mStringBuilder.toString(), mIsItalic, mIsSmallCaps);
                mStringBuilder.setLength(0);
                mBlock.addSpan(span);
            }
        }

    }
}

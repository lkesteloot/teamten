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
import java.util.List;
import java.util.stream.Collectors;

/**
 * A block is a paragraph (similar to a block in HTML DOM). It's a sequence of spans.
 */
public class Block {
    private final BlockType mBlockType;
    private final int mCounter;
    private final List<Span> mSpans = new ArrayList<>();

    public Block(BlockType blockType, int counter) {
        mBlockType = blockType;
        mCounter = counter;
    }

    public BlockType getBlockType() {
        return mBlockType;
    }

    /**
     * Get the counter for numbered lists.
     */
    public int getCounter() {
        return mCounter;
    }

    public List<Span> getSpans() {
        return mSpans;
    }

    /**
     * Return a string version of all text spans. Ignores non-text spans.
     */
    public String getText() {
        return mSpans.stream()
                .filter((span) -> span instanceof TextSpan)
                .map((span) -> (TextSpan) span)
                .map(TextSpan::getText)
                .collect(Collectors.joining());
    }

    public void addSpan(Span span) {
        mSpans.add(span);
    }

    @Override // Object
    public String toString() {
        if (mSpans.isEmpty()) {
            return "No spans";
        } else {
            String first = getText();
            first = first.substring(0, Math.min(30, first.length()));

            return String.format("%s, %d spans, starting with: %s ...",
                    mBlockType, mSpans.size(), first);
        }
    }

    /**
     * Make a builder for numbered lists.
     */
    public static Builder numberedListBuilder(int counter) {
        return new Builder(BlockType.NUMBERED_LIST, counter);
    }

    /**
     * Builds a Block one character at a time.
     */
    public static class Builder {
        private final Block mBlock;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private boolean mInsideQuotation;
        private boolean mIsBold;
        private boolean mIsItalic;
        private boolean mIsSmallCaps;

        private Builder(BlockType blockType, int counter) {
            mBlock = new Block(blockType, counter);
        }

        public Builder(BlockType blockType) {
            this(blockType, 0);
        }

        /**
         * Add the character to the block.
         *
         * @param ch the character to add.
         * @param isBold whether the character should be displayed in bold.
         * @param isItalic whether the character should be displayed in italics.
         * @param isSmallCaps whether the character should be displayed in small caps.
         */
        public void addText(char ch, boolean isBold, boolean isItalic, boolean isSmallCaps) {
            // Simple character translations.
            if (mBlock.getBlockType() != BlockType.CODE) {
                if (ch == '~') {
                    // No-break space.
                    ch = '\u00A0';
                } else if (ch == '\'') {
                    ch = '’';
                } else if (ch == '"') {
                    ch = mInsideQuotation ? '”' : '“';
                    mInsideQuotation = !mInsideQuotation;
                }
            }

            if (isBold != mIsBold || isItalic != mIsItalic || isSmallCaps != mIsSmallCaps) {
                emitSpan();
                mIsBold = isBold;
                mIsItalic = isItalic;
                mIsSmallCaps = isSmallCaps;
            }

            mStringBuilder.append(ch);
        }

        /**
         * Add any span to this block.
         */
        public void addSpan(Span span) {
            emitSpan();
            mBlock.addSpan(span);
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
            if (mInsideQuotation) {
                System.out.println("Warning: Block ends without closing quotation: " + mBlock);
            }
            return mBlock;
        }

        /**
         * Possibly emit span, if we have characters accumulated up.
         */
        private void emitSpan() {
            if (mStringBuilder.length() > 0) {
                TextSpan span = new TextSpan(mStringBuilder.toString(), mIsBold, mIsItalic, mIsSmallCaps);
                mStringBuilder.setLength(0);
                mBlock.addSpan(span);
            }
        }

    }
}

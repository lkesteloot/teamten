
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
        public void addText(char ch, boolean isItalic, boolean isSmallCaps) {
            if (isItalic != mIsItalic || isSmallCaps != mIsSmallCaps) {
                emitSpan();
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
            return mBlock;
        }

        /**
         * Possibly emit span, if we have characters accumulated up.
         */
        private void emitSpan() {
            if (mStringBuilder.length() > 0) {
                TextSpan span = new TextSpan(mStringBuilder.toString(), mIsItalic, mIsSmallCaps);
                mStringBuilder.setLength(0);
                mBlock.addSpan(span);
            }
        }

    }
}

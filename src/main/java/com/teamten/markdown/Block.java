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

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A block is a paragraph (similar to a block in HTML DOM). It's a sequence of spans.
 */
public class Block {
    private final @NotNull BlockType mBlockType;
    private final int mCounter;
    private final @NotNull List<Span> mSpans = new ArrayList<>();

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

    /**
     * Convert apostrophes, quotes, etc.
     */
    public void postProcessText() {
        if (getBlockType() != BlockType.CODE) {
            boolean insideQuotation = false;
            int previousCh = -1;

            for (int i = 0; i < mSpans.size(); i++) {
                Span span = mSpans.get(i);
                if (span instanceof TextSpan) {
                    TextSpan textSpan = (TextSpan) span;
                    StringBuilder builder = new StringBuilder();
                    String text = textSpan.getText();

                    for (int j = 0; j < text.length(); ) {
                        int ch = text.codePointAt(j);

                        // Simple character translations.
                        if (ch == '~') {
                            // No-break space.
                            builder.append('\u00A0');
                        } else if (ch == '\'') {
                            builder.append('’');
                        } else if (ch == '"') {
                            // TODO make this configurable.
//                            builder.append(insideQuotation ? '”' : '“');
                            builder.append(insideQuotation ? "\u00A0»" : "«\u00A0");
                            insideQuotation = !insideQuotation;
                        } else {
                            builder.appendCodePoint(ch);
                        }

                        previousCh = ch;
                        j += Character.charCount(ch);
                    }

                    mSpans.set(i, new TextSpan(builder.toString(), textSpan.isBold(), textSpan.isItalic(),
                            textSpan.isSmallCaps(), textSpan.isCode()));
                }
            }

            if (insideQuotation) {
                System.out.println("Warning: Block ends without closing quotation: " + this);
            }
        }
    }

    /**
     * Return just the text of the block.
     */
    public String toBriefString() {
        // Join the individual spans.
        String s = mSpans.stream()
                .map(Span::toString)
                .collect(Collectors.joining());

        // Prefix counter if it's a numbered list.
        if (mBlockType == BlockType.NUMBERED_LIST) {
            s = mCounter + ". " + s;
        }

        return s;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Block block = (Block) o;

        if (mCounter != block.mCounter) {
            return false;
        }
        if (mBlockType != block.mBlockType) {
            return false;
        }
        return mSpans.equals(block.mSpans);
    }

    @Override
    public int hashCode() {
        int result = mBlockType.hashCode();
        result = 31*result + mCounter;
        result = 31*result + mSpans.hashCode();
        return result;
    }

    /**
     * Make a builder for numbered lists.
     */
    public static Builder numberedListBuilder(int counter) {
        return new Builder(BlockType.NUMBERED_LIST, counter);
    }

    /**
     * Make a plain body block from a string.
     */
    public static Block bodyBlock(String text) {
        Span span = new TextSpan(text, false, false, false, false);
        return new Builder(BlockType.BODY).addSpan(span).build();
    }

    /**
     * Builds a Block one character at a time.
     */
    public static class Builder {
        private final Block mBlock;
        private final StringBuilder mStringBuilder = new StringBuilder();
        private boolean mIsBold;
        private boolean mIsItalic;
        private boolean mIsSmallCaps;
        private boolean mIsCode;

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
         * @param isCode whether the character should be displayed as code.
         */
        public void addText(char ch, boolean isBold, boolean isItalic, boolean isSmallCaps, boolean isCode) {
            // Switch style if necessary.
            if (isBold != mIsBold || isItalic != mIsItalic || isSmallCaps != mIsSmallCaps || isCode != mIsCode) {
                emitSpan();
                mIsBold = isBold;
                mIsItalic = isItalic;
                mIsSmallCaps = isSmallCaps;
                mIsCode = isCode;
            }

            mStringBuilder.append(ch);
        }

        /**
         * Add a string. See {@link #addText(char, boolean, boolean, boolean, boolean)} for details.
         */
        public void addText(String text, boolean isBold, boolean isItalic, boolean isSmallCaps, boolean isCode) {
            for (char ch : text.toCharArray()) {
                addText(ch, isBold, isItalic, isSmallCaps, isCode);
            }
        }

        /**
         * Add a plain string (no markup).
         */
        public void addText(String text) {
            addText(text, false, false, false, false);
        }

        /**
         * Add any span to this block.
         */
        public Builder addSpan(Span span) {
            emitSpan();
            mBlock.addSpan(span);

            return this;
        }

        /**
         * Add a whole block to this block. The block's type and counter are ignored.
         */
        public Builder addBlock(Block block) {
            // Add each span.
            block.getSpans().forEach(this::addSpan);
            return this;
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

            // Note that we can't warn if the block is built while inside bold or italic, because we
            // can't be sure that those weren't closed. If the last character of a block is the
            // character to stop bold, we'll never know about it because we won't get another
            // character that's not bold.

            return mBlock;
        }

        /**
         * Possibly emit span, if we have characters accumulated up.
         */
        private void emitSpan() {
            if (mStringBuilder.length() > 0) {
                TextSpan span = new TextSpan(mStringBuilder.toString(), mIsBold, mIsItalic, mIsSmallCaps, mIsCode);
                mStringBuilder.setLength(0);
                mBlock.addSpan(span);
            }
        }
    }
}

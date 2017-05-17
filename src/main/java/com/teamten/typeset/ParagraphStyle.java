/*
 *
 *    Copyright 2017 Lawrence Kesteloot
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

import com.teamten.font.FontManager;
import com.teamten.font.FontPack;
import com.teamten.font.TypefaceVariantSize;
import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Describes various layout attributes about a paragraph.
 */
public class ParagraphStyle {
    private final boolean mCenter;
    private final boolean mNewPage;
    private final boolean mOddPage;
    private final boolean mOwnPage;
    private final boolean mAllowLineBreaks;
    private final long mMarginTop;
    private final long mMarginBottom;
    private final boolean mResetFootnoteNumber;
    private final long mLeading;
    private final long mParagraphIndent;
    private final long mFirstLineIndent;
    private final long mSubsequentLinesIndent;
    private final boolean mPreventBreak;
    private final FontPack mFontPack;

    public ParagraphStyle(boolean center, boolean newPage, boolean oddPage,
                          boolean ownPage, boolean allowLineBreaks, long marginTop, long marginBottom,
                          boolean resetFootnoteNumber, long leading,
                          long paragraphIndent, long firstLineIndent, long subsequentLinesIndent,
                          boolean preventBreak, FontPack fontPack) {

        mCenter = center;
        mNewPage = newPage;
        mOddPage = oddPage;
        mOwnPage = ownPage;
        mAllowLineBreaks = allowLineBreaks;
        mMarginTop = marginTop;
        mMarginBottom = marginBottom;
        mResetFootnoteNumber = resetFootnoteNumber;
        mLeading = leading;
        mParagraphIndent = paragraphIndent;
        mFirstLineIndent = firstLineIndent;
        mSubsequentLinesIndent = subsequentLinesIndent;
        mPreventBreak = preventBreak;
        mFontPack = fontPack;
    }

    /**
     * Whether to center the whole paragraph.
     */
    public boolean isCenter() {
        return mCenter;
    }

    /**
     * Whether the block should start a new page.
     */
    public boolean isNewPage() {
        return mNewPage;
    }

    /**
     * Whether the block should start a new odd page (skipping an even page if necessary).
     */
    public boolean isOddPage() {
        return mOddPage;
    }

    /**
     * Whether the block should be on its own page (starting a new page and ejecting afterward).
     */
    public boolean isOwnPage() {
        return mOwnPage;
    }

    /**
     * Whether to allow line breaks in this paragraph. For example, we don't want line breaks in code.
     */
    public boolean isAllowLineBreaks() {
        return mAllowLineBreaks;
    }

    /**
     * The margin above the paragraph.
     */
    public long getMarginTop() {
        return mMarginTop;
    }

    /**
     * The margin below the paragraph.
     */
    public long getMarginBottom() {
        return mMarginBottom;
    }

    /**
     * Whether to reset the footnote number to 1 before processing this paragraph. This is done for
     * new chapters.
     */
    public boolean isResetFootnoteNumber() {
        return mResetFootnoteNumber;
    }

    /**
     * The leading for paragraph lines.
     */
    public long getLeading() {
        return mLeading;
    }

    /**
     * Indent size, whether used or not.
     */
    public long getParagraphIndent() {
        return mParagraphIndent;
    }

    /**
     * Indent for the first line.
     */
    public long getFirstLineIndent() {
        return mFirstLineIndent;
    }

    /**
     * Indent for all lines but the first.
     */
    public long getSubsequentLinesIndent() {
        return mSubsequentLinesIndent;
    }

    /**
     * Whether to prevent a page break after this paragraph (for titles).
     */
    public boolean preventBreak() {
        return mPreventBreak;
    }

    /**
     * The font pack for this paragraph.
     */
    public FontPack getFontPack() {
        return mFontPack;
    }

    /**
     * Creates an OutputShape object corresponding to this style for the specified body width.
     */
    public OutputShape makeOutputShape(long bodyWidth) {
        return OutputShape.singleLine(bodyWidth, mFirstLineIndent, mSubsequentLinesIndent);
    }

    /**
     * Return a new paragraph size, identical except the font pack is scaled by the specified amount.
     */
    public ParagraphStyle withScaledFont(double scale) {
        return new ParagraphStyle(
                mCenter,
                mNewPage,
                mOddPage,
                mOwnPage,
                mAllowLineBreaks,
                mMarginTop,
                mMarginBottom,
                mResetFootnoteNumber,
                (long) (mLeading*scale),
                mParagraphIndent,
                mFirstLineIndent,
                mSubsequentLinesIndent,
                mPreventBreak,
                mFontPack.withScaledFont(scale));
    }

    public static ParagraphStyle forBlock(Block block, BlockType previousBlockType,
                                          Config config, FontManager fontManager) {

        Config.Key regularFontKey;
        // Move this to the switch statement if we end up with code in headers, captions, etc.:
        Config.Key codeFontKey = Config.Key.BODY_CODE_FONT;
        boolean center = false;
        boolean newPage = false;
        boolean oddPage = false;
        boolean ownPage = false;
        boolean addTracking = false;
        boolean allowLineBreaks = true;
        long marginTop = 0;
        long marginBottom = 0;
        boolean resetFootnoteNumber = false;
        int firstLineIndentCount = 0;
        int subsequentLinesIndentCount = 0;
        boolean preventBreak = false;

        switch (block.getBlockType()) {
            case BODY:
                regularFontKey = Config.Key.BODY_FONT;
                firstLineIndentCount = previousBlockType == BlockType.BODY ? 1 : 0;
                if (previousBlockType == BlockType.NUMBERED_LIST ||
                        previousBlockType == BlockType.BULLET_LIST) {

                    marginTop = PT.toSp(4.0);
                } else if (previousBlockType == BlockType.POETRY ||
                        previousBlockType == BlockType.BLOCK_QUOTE) {

                    marginTop = PT.toSp(8.0);
                } else if (previousBlockType == BlockType.SIGNATURE) {
                    marginTop = PT.toSp(32.0);
                }
                break;

            case BLOCK_QUOTE:
                regularFontKey = Config.Key.BLOCK_QUOTE_FONT;
                firstLineIndentCount = previousBlockType == BlockType.BLOCK_QUOTE ? 2 : 1;
                subsequentLinesIndentCount = 1;
                if (previousBlockType != BlockType.BLOCK_QUOTE) {
                    marginTop = PT.toSp(8.0);
                }
                break;

            case PART_HEADER:
                regularFontKey = Config.Key.PART_HEADER_FONT;
                center = true;
                marginTop = IN.toSp(1.75);
                oddPage = true;
                ownPage = true;
                addTracking = true;
                resetFootnoteNumber = true;
                preventBreak = true;
                break;

            case CHAPTER_HEADER:
            case MINOR_SECTION_HEADER:
                regularFontKey = Config.Key.CHAPTER_HEADER_FONT;
                center = true;
                marginTop = IN.toSp(0.75);
                marginBottom = IN.toSp(0.75);
                oddPage = true;
                addTracking = true;
                resetFootnoteNumber = true;
                preventBreak = true;
                break;

            case MINOR_HEADER:
                regularFontKey = Config.Key.MINOR_HEADER_FONT;
                center = true;
                marginTop = IN.toSp(0.25);
                marginBottom = IN.toSp(0.10);
                preventBreak = true;
                break;

            case NUMBERED_LIST:
                regularFontKey = Config.Key.BODY_FONT;
                if (previousBlockType != BlockType.NUMBERED_LIST) {
                    marginTop = PT.toSp(8.0);
                }
                marginBottom = PT.toSp(4.0);
                subsequentLinesIndentCount = 1;
                break;

            case BULLET_LIST:
                regularFontKey = Config.Key.BODY_FONT;
                if (previousBlockType != BlockType.BULLET_LIST) {
                    marginTop = PT.toSp(8.0);
                }
                marginBottom = PT.toSp(4.0);
                subsequentLinesIndentCount = 1;
                break;

            case CODE:
                regularFontKey = Config.Key.CODE_FONT;
                if (previousBlockType != BlockType.CODE) {
                    marginTop = PT.toSp(8.0);
                }
                firstLineIndentCount = 1;
                allowLineBreaks = false;
                break;

            case OUTPUT:
                regularFontKey = Config.Key.OUTPUT_FONT;
                if (!previousBlockType.isConsole()) {
                    marginTop = PT.toSp(8.0);
                }
                firstLineIndentCount = 1;
                break;

            case INPUT:
                regularFontKey = Config.Key.INPUT_FONT;
                if (!previousBlockType.isConsole()) {
                    marginTop = PT.toSp(8.0);
                }
                firstLineIndentCount = 1;
                break;

            case POETRY:
                regularFontKey = Config.Key.BODY_FONT;
                if (previousBlockType != BlockType.POETRY) {
                    marginTop = PT.toSp(8.0);
                }
                firstLineIndentCount = 1;
                subsequentLinesIndentCount = 2;
                break;

            case SIGNATURE:
                regularFontKey = Config.Key.BODY_FONT;
                if (previousBlockType != BlockType.SIGNATURE) {
                    marginTop = PT.toSp(32.0);
                }
                firstLineIndentCount = 0;
                subsequentLinesIndentCount = 0;
                break;

            case CAPTION:
                regularFontKey = Config.Key.CAPTION_FONT;
                center = true;
                break;

            default:
                throw new IllegalArgumentException("Unknown block type " + block.getBlockType());
        }

        // Margin below code blocks.
        if (block.getBlockType() != BlockType.CODE && previousBlockType == BlockType.CODE) {
            marginTop = Math.max(marginTop, PT.toSp(8.0));
        }
        if (!block.getBlockType().isConsole() && previousBlockType != null && previousBlockType.isConsole()) {
            marginTop = Math.max(marginTop, PT.toSp(8.0));
        }

        TypefaceVariantSize regularFontDesc = config.getFont(regularFontKey);
        TypefaceVariantSize codeFontDesc = config.getFont(codeFontKey);

        FontPack fontPack = FontPack.create(fontManager, regularFontDesc, codeFontDesc);
        double fontSize = regularFontDesc.getSize();

        if (addTracking) {
            // Add tracking (space between letters).
            fontPack = fontPack.withTracking(0.1, 0.5);
        }

        // 135% recommended by http://practicaltypography.com/line-spacing.html
        long leading = PT.toSp(fontSize*1.35f);
        long paragraphIndent = PT.toSp(fontSize*2);

        return new ParagraphStyle(center, newPage, oddPage, ownPage, allowLineBreaks, marginTop,
                marginBottom, resetFootnoteNumber, leading, paragraphIndent,
                paragraphIndent*firstLineIndentCount,
                paragraphIndent*subsequentLinesIndentCount, preventBreak,
                fontPack);
    }
}

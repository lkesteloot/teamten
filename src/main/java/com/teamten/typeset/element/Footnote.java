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

package com.teamten.typeset.element;

import com.teamten.font.FontManager;
import com.teamten.hyphen.HyphenDictionary;
import com.teamten.markdown.Block;
import com.teamten.markdown.BlockType;
import com.teamten.typeset.Config;
import com.teamten.typeset.HorizontalList;
import com.teamten.typeset.OutputShape;
import com.teamten.typeset.ParagraphStyle;
import com.teamten.typeset.Typesetter;
import com.teamten.typeset.VerticalList;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Represents a footnote that will be displayed at the bottom of the page.
 */
public class Footnote extends VBox {
    private final long mBaselineSkip;
    private final long mFirstHBoxHeight;
    private final long mLastHBoxDepth;

    private Footnote(List<Element> elements, long baselineSkip, long firstHBoxHeight, long lastHBoxDepth) {
        super(elements);
        mBaselineSkip = baselineSkip;
        mFirstHBoxHeight = firstHBoxHeight;
        mLastHBoxDepth = lastHBoxDepth;
    }

    /**
     * Get the distance between baselines in this footnote.
     */
    public long getBaselineSkip() {
        return mBaselineSkip;
    }

    /**
     * Get the height of the first HBox in this footnote.
     */
    public long getFirstHBoxHeight() {
        return mFirstHBoxHeight;
    }

    /**
     * Get the depth of the last HBox in this footnote.
     */
    public long getLastHBoxDepth() {
        return mLastHBoxDepth;
    }

    @NotNull
    public static Footnote create(HBox mark, Block block, Config config, FontManager fontManager,
                                  HyphenDictionary hyphenDictionary) throws IOException {

        // Only plain body for footnotes.
        if (block.getBlockType() != BlockType.BODY) {
            throw new IllegalArgumentException("Footnote block must be of type BODY");
        }

        // Get the style for this paragraph given its block type.
        ParagraphStyle paragraphStyle = ParagraphStyle.forBlock(block, null, config, fontManager);

        // Make a vertical list for the footnote.
        VerticalList verticalList = new VerticalList();

        // Set the distance between baselines based on the paragraph's main font.
        verticalList.setBaselineSkip(paragraphStyle.getLeading());

        // Create a horizontal list for this paragraph.
        HorizontalList horizontalList = Typesetter.makeHorizontalListFromBlock(block, paragraphStyle, null, config,
                fontManager, hyphenDictionary, 0);

        // Prepend footnote.
        List<Element> elements = horizontalList.getElements();
        elements.add(0, mark);

        // Break the horizontal list into HBox elements, adding them to the vertical list.
        long bodyWidth = config.getBodyWidth();
        OutputShape outputShape = OutputShape.singleLine(bodyWidth, paragraphStyle.getParagraphIndent(), 0);

        horizontalList.format(verticalList, outputShape);

        return new Footnote(verticalList.getElements(),
                verticalList.getBaselineSkip(),
                verticalList.getFirstHBoxHeight(),
                verticalList.getLastHBoxDepth());
    }
}

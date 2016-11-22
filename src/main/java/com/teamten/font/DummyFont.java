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

package com.teamten.font;

import com.teamten.typeset.Ligatures;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * A font for testing.
 */
public class DummyFont extends AbstractFont {
    private final long mWidth;
    private final long mHeight;
    private final long mDepth;

    /**
     * Dummy font with reasonable sizes.
     *
     * @param ligatures the ligature list, or null for none.
     */
    public DummyFont(Ligatures ligatures) {
        this(ligatures, PT.toSp(0.5), PT.toSp(0.8), PT.toSp(0.2));
    }

    /**
     * @param ligatures the ligature list, or null for none.
     * @param width width of every character for a 1pt font.
     * @param height height of every character for a 1pt font.
     * @param depth depth of every character for a 1pt font.
     */
    public DummyFont(Ligatures ligatures, long width, long height, long depth) {
        super(ligatures);
        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }

    @Override
    public long getSpaceWidth() {
        return mWidth;
    }

    @Override
    public Metrics getCharacterMetrics(int ch, double fontSize) {
        return new Metrics((long) (mWidth*fontSize), (long) (mHeight*fontSize), (long) (mDepth*fontSize));
    }

    @Override
    public void draw(String text, double fontSize, long x, long y, PDPageContentStream contents) throws IOException {
        // Don't draw.
    }

    @Override
    public String toString() {
        return "DummyFont";
    }
}

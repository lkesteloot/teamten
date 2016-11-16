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

package com.teamten.typeset.element;

import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;
import java.io.PrintStream;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents a kerning adjustment.
 */
public class Kern extends DiscardableElement {
    private final long mAmount;
    private final boolean mIsHorizontal;

    public Kern(long amount, boolean isHorizontal) {
        mAmount = amount;
        mIsHorizontal = isHorizontal;
    }

    /**
     * The amount to adjust. This is normally negative.
     */
    public long getAmount() {
        return mAmount;
    }

    /**
     * Whether this kern is intended to be horizontal (between letters) or vertical (between lines). It's normally
     * horizontal.
     */
    public boolean isHorizontal() {
        return mIsHorizontal;
    }

    @Override
    public long getWidth() {
        return mIsHorizontal ? mAmount : 0;
    }

    @Override
    public long getHeight() {
        return mIsHorizontal ? 0 : mAmount;
    }

    @Override
    public long getDepth() {
        return 0;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        return getWidth();
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        return getHeight();
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%s%s kern: %.1fpt%n", indent, mIsHorizontal ? "Horizontal" : "Vertical", PT.fromSp(mAmount));
    }

    @Override
    public String toTextString() {
        return String.format("<Kern %.1fpt>", PT.fromSp(mAmount));
    }
}

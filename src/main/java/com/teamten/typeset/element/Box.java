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

import com.teamten.typeset.Dimensions;
import com.teamten.typeset.PdfUtil;
import org.apache.pdfbox.pdmodel.PDPageContentStream;

import java.io.IOException;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents a 2D box of something, like a word or an image. Can be used by itself to make an empty space.
 */
public class Box extends NonDiscardableElement {
    private final long mWidth;
    private final long mHeight;
    private final long mDepth;
    private final long mShift;

    public Box(long width, long height, long depth, long shift) {
        mWidth = width;
        mHeight = height;
        mDepth = depth;
        mShift = shift;
    }

    public Box(long width, long height, long depth) {
        this(width, height, depth, 0);
    }

    protected Box(Dimensions dimensions, long shift) {
        this(dimensions.getWidth(), dimensions.getHeight(), dimensions.getDepth(), shift);
    }

    protected Box(Dimensions dimensions) {
        this(dimensions, 0);
    }

    /**
     * Draw a debug rectangle at specified point, which represents the left-most point at the baseline.
     */
    protected void drawDebugRectangle(PDPageContentStream contents, long x, long y) throws IOException {
        if (DRAW_DEBUG) {
            PdfUtil.drawDebugRectangle(contents, x, y, getWidth(), getHeight());
            PdfUtil.drawDebugRectangle(contents, x, y - getDepth(), getWidth(), getDepth());
        }
    }

    /**
     * The width (horizontally) of this box.
     */
    @Override
    public long getWidth() {
        return mWidth;
    }

    /**
     * The distance from the baseline to the top of the box.
     */
    @Override
    public long getHeight() {
        return mHeight;
    }

    /**
     * The distance from the baseline to the bottom of the box.
     */
    @Override
    public long getDepth() {
        return mDepth;
    }

    /**
     * The amount to shift this box, to the right during vertical layout and up during horizontal layout.
     */
    public long getShift() {
        return mShift;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        return mWidth;
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        return mHeight + mDepth;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + getDimensionString();
    }

    /**
     * A string that specifies the three dimensions of the box.
     */

    protected String getDimensionString() {
        return String.format("(%.1fpt, %.1fpt, %.1fpt)", PT.fromSp(mWidth), PT.fromSp(mHeight), PT.fromSp(mDepth));
    }

}

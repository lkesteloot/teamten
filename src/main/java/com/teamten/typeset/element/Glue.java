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
 * Whitespace that has a default width but can be shrunk or stretched.
 */
public class Glue extends DiscardableElement implements Flexible {
    private final long mSize;
    private final Flexibility mStretch;
    private final Flexibility mShrink;
    private final boolean mIsHorizontal;

    /**
     * All units are in scaled points.
     *
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added.
     * @param shrink the maximum extra space that can be removed.
     */
    public Glue(long size, Flexibility stretch, Flexibility shrink, boolean isHorizontal) {
        mSize = size;
        mStretch = stretch;
        mShrink = shrink;
        mIsHorizontal = isHorizontal;
    }

    /**
     * All units are in scaled points.
     *
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added (not infinite).
     * @param shrink the maximum extra space that can be removed (not infinite).
     */
    public Glue(long size, long stretch, boolean stretchIsInfinite, long shrink, boolean shrinkIsInfinite, boolean isHorizontal) {
        this(size, new Flexibility(stretch, stretchIsInfinite), new Flexibility(shrink, shrinkIsInfinite), isHorizontal);
    }

    /**
     * Convenience constructor for non-infinite glue. All units are in scaled points.
     *
     * @param size the ideal size of the glue.
     * @param stretch the maximum extra space that can be added (not infinite).
     * @param shrink the maximum extra space that can be removed (not infinite).
     */
    public Glue(long size, long stretch, long shrink, boolean isHorizontal) {
        this(size, stretch, false, shrink, false, isHorizontal);
    }

    /**
     * Return fixed horizontal glue.
     */
    public static Glue horizontal(long size) {
        return new Glue(size, 0, 0, true);
    }

    /**
     * Return fixed vertical glue.
     */
    public static Glue vertical(long size) {
        return new Glue(size, 0, 0, false);
    }

    /**
     * Return infinite glue of zero size.
     */
    public static Glue infinite(boolean isHorizontal) {
        return new Glue(0, PT.toSp(1.0), true, 0, false, isHorizontal);
    }

    /**
     * Return infinite horizontal glue of zero width.
     */
    public static Glue infiniteHorizontal() {
        return infinite(true);
    }

    /**
     * Return infinite vertical glue of zero height.
     */
    public static Glue infiniteVertical() {
        return infinite(false);
    }

    @Override
    public long getSize() {
        return mSize;
    }

    @Override
    public Flexibility getStretch() {
        return mStretch;
    }

    @Override
    public Flexibility getShrink() {
        return mShrink;
    }

    @Override
    public Glue fixed(long newSize) {
        return new Glue(newSize, 0, 0, mIsHorizontal);
    }

    public boolean isHorizontal() {
        return mIsHorizontal;
    }

    @Override
    public long getWidth() {
        return mIsHorizontal ? mSize : 0;
    }

    @Override
    public long getHeight() {
        return mIsHorizontal ? 0 : mSize;
    }

    @Override
    public long getDepth() {
        return 0;
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        // Assume that we've been "set" and that the stretch or shrink has been determined and put into size.
        return mSize;
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        // Assume that we've been "set" and that the stretch or shrink has been determined and put into size.
        return mSize;
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.printf("%s%s glue: %.1fpt%s%s%n", indent, mIsHorizontal ? "Horizontal" : "Vertical",
                PT.fromSp(mSize), mStretch.toString("+"), mShrink.toString("-"));
    }

    @Override
    public String toTextString() {
        return " ";
    }

}

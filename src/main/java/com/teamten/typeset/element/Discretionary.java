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

/**
 * Represents a discretionary line break, storing both the split and unsplit versions of the text.
 */
public class Discretionary extends NonDiscardableElement {
    public static final int HYPHEN_PENALTY = 50;
    private final HBox mPreBreak;
    private final HBox mPostBreak;
    private final HBox mNoBreak;
    private final int mPenalty;

    public Discretionary(HBox preBreak, HBox postBreak, HBox noBreak, int penalty) {
        mPreBreak = preBreak;
        mPostBreak = postBreak;
        mNoBreak = noBreak;
        mPenalty = penalty;
    }

    public HBox getPreBreak() {
        return mPreBreak;
    }

    public HBox getPostBreak() {
        return mPostBreak;
    }

    public HBox getNoBreak() {
        return mNoBreak;
    }

    public int getPenalty() {
        return mPenalty;
    }

    @Override
    public long getWidth() {
        throw new IllegalStateException("discretionary breaks don't have a size");
    }

    @Override
    public long getHeight() {
        throw new IllegalStateException("discretionary breaks don't have a size");
    }

    @Override
    public long getDepth() {
        throw new IllegalStateException("discretionary breaks don't have a size");
    }

    @Override
    public long layOutHorizontally(long x, long y, PDPageContentStream contents) throws IOException {
        throw new IllegalStateException("discretionary elements should be not laid out horizontally");
    }

    @Override
    public long layOutVertically(long x, long y, PDPageContentStream contents) throws IOException {
        throw new IllegalStateException("discretionary elements should be not laid out vertically");
    }

    @Override
    public void println(PrintStream stream, String indent) {
        stream.print(indent);
        stream.println(toString());
    }

    @Override
    public String toTextString() {
        return mNoBreak.toTextString();
    }

    @Override
    public String toString() {
        return String.format("Discretionary: split as \"%s\" and \"%s\" or whole as \"%s\"",
                mPreBreak.toTextString(), mPostBreak.toTextString(), mNoBreak.toTextString());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Discretionary that = (Discretionary) o;

        if (mPenalty != that.mPenalty) {
            return false;
        }
        if (!mPreBreak.equals(that.mPreBreak)) {
            return false;
        }
        if (!mPostBreak.equals(that.mPostBreak)) {
            return false;
        }
        return mNoBreak.equals(that.mNoBreak);

    }

    @Override
    public int hashCode() {
        int result = mPreBreak.hashCode();
        result = 31*result + mPostBreak.hashCode();
        result = 31*result + mNoBreak.hashCode();
        result = 31*result + mPenalty;
        return result;
    }
}

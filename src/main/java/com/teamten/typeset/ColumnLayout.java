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

package com.teamten.typeset;

/**
 * Describes the number and sizes of columns.
 */
public class ColumnLayout {
    private final int mColumnCount;
    private final long mColumnWidth;
    private final long mMargin;

    /**
     * Create a new column layout.
     *
     * @param columnCount the number of columns.
     * @param columnWidth the width of each column. Can be 0 for a single column.
     * @param margin the space between columns.
     */
    public ColumnLayout(int columnCount, long columnWidth, long margin) {
        mColumnCount = columnCount;
        mColumnWidth = columnWidth;
        mMargin = margin;
    }

    /**
     * Create a new single-column layout of zero width.
     */
    public static ColumnLayout single() {
        // We don't care about the width for single columns.
        return new ColumnLayout(1, 0, 0);
    }

    /**
     * Create a new single-column layout of the specified width.
     */
    public static ColumnLayout single(long width) {
        // We don't care about the width for single columns.
        return new ColumnLayout(1, width, 0);
    }

    /**
     * Create a multi-column layout from the specified body width and margin.
     */
    public static ColumnLayout fromBodyWidth(int columnCount, long bodyWidth, long margin) {
        long columnWidth = (bodyWidth - margin*(columnCount - 1))/columnCount;
        return new ColumnLayout(columnCount, columnWidth, margin);
    }

    public int getColumnCount() {
        return mColumnCount;
    }

    public long getColumnWidth() {
        return mColumnWidth;
    }

    public long getMargin() {
        return mMargin;
    }

    @Override
    public String toString() {
        return "ColumnLayout{" +
                "mColumnCount=" + mColumnCount +
                ", mColumnWidth=" + mColumnWidth +
                ", mMargin=" + mMargin +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        ColumnLayout that = (ColumnLayout) o;

        if (mColumnCount != that.mColumnCount) {
            return false;
        }
        if (mColumnWidth != that.mColumnWidth) {
            return false;
        }
        return mMargin == that.mMargin;

    }

    @Override
    public int hashCode() {
        int result = mColumnCount;
        result = 31*result + (int) (mColumnWidth ^ (mColumnWidth >>> 32));
        result = 31*result + (int) (mMargin ^ (mMargin >>> 32));
        return result;
    }
}

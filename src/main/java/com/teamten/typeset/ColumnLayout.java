package com.teamten.typeset;

/**
 * Describes the number and sizes of columns.
 */
public class ColumnLayout {
    private final int mColumnCount;
    private final long mColumnWidth;
    private final long mMargin;

    public ColumnLayout(int columnCount, long columnWidth, long margin) {
        mColumnCount = columnCount;
        mColumnWidth = columnWidth;
        mMargin = margin;
    }

    /**
     * Create a new single-column layout.
     */
    public static ColumnLayout single() {
        // We don't care about the width for single columns.
        return new ColumnLayout(1, 0, 0);
    }

    /**
     * Create a multi-column layout from the specified body width and margin.
     */
    public static ColumnLayout fromBodyWidth(int columnCount, long bodyWidth, long margin) {
        long columnWidth = (bodyWidth - margin*(columnCount - 1)) / columnCount;
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
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ColumnLayout that = (ColumnLayout) o;

        if (mColumnCount != that.mColumnCount) return false;
        if (mColumnWidth != that.mColumnWidth) return false;
        return mMargin == that.mMargin;

    }

    @Override
    public int hashCode() {
        int result = mColumnCount;
        result = 31 * result + (int) (mColumnWidth ^ (mColumnWidth >>> 32));
        result = 31 * result + (int) (mMargin ^ (mMargin >>> 32));
        return result;
    }
}
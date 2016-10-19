package com.teamten.typeset;

/**
 * A bookmark for a section, such as a part or chapter.
 */
public class SectionBookmark extends Bookmark {
    private final String mName;

    public SectionBookmark(String name) {
        mName = name;
    }

    public String getName() {
        return mName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SectionBookmark that = (SectionBookmark) o;

        return mName.equals(that.mName);

    }

    @Override
    public int hashCode() {
        return mName.hashCode();
    }

    @Override
    public String toString() {
        return "Section \"" + mName + "\"";
    }
}

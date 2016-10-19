package com.teamten.typeset;

/**
 * A bookmark for a section, such as a part or chapter.
 */
public class SectionBookmark extends Bookmark {
    private final Type mType;
    private final String mName;

    public enum Type {
        PART("Part"),
        CHAPTER("Chapter");

        private final String mLabel;

        Type(String label) {
            mLabel = label;
        }

        @Override
        public String toString() {
            return mLabel;
        }
    }

    public SectionBookmark(Type type, String name) {
        mType = type;
        mName = name;
    }

    public Type getType() {
        return mType;
    }

    public String getName() {
        return mName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SectionBookmark that = (SectionBookmark) o;

        if (mType != that.mType) return false;
        return mName.equals(that.mName);

    }

    @Override
    public int hashCode() {
        int result = mType.hashCode();
        result = 31 * result + mName.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return mType + " \"" + mName + "\"";
    }
}

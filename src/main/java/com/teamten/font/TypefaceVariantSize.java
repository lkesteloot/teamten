package com.teamten.font;

import com.teamten.typeset.SpaceUnit;

/**
 * Represents a typeface, a variant (e.g., bold), and a size (e.g., 11pt).
 */
public class TypefaceVariantSize extends TypefaceVariant {
    private final double mSize;

    public TypefaceVariantSize(Typeface typeface, FontVariant fontVariant, double size) {
        super(typeface, fontVariant);
        mSize = size;
    }

    /**
     * Get the font size in points.
     */
    public double getSize() {
        return mSize;
    }

    /**
     * Return a new instance with the specified variant.
     */
    public TypefaceVariantSize withVariant(FontVariant fontVariant) {
        return new TypefaceVariantSize(getTypeface(), fontVariant, getSize());
    }

    @Override
    public String toString() {
        return getTypeface() + ", " + getFontVariant() + ", " + mSize + "pt";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        TypefaceVariantSize that = (TypefaceVariantSize) o;

        return Double.compare(that.mSize, mSize) == 0;

    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        long temp;
        temp = Double.doubleToLongBits(mSize);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    /**
     * Parses a string as a typeface, variant, and size. The format must be "typeface, variant, size", where
     * "typeface" can be parsed by {@link Typeface#parse(String)}, "variant" can be parsed by
     * {@link FontVariant#parse(String)}, and the size can be parsed by {@link SpaceUnit#parseDistance(String)}.
     *
     * @throws IllegalArgumentException if the string cannot be parsed.
     */
    public static TypefaceVariantSize valueOf(String s) {
        String[] parts = s.split(",");

        if (parts.length != 3) {
            throw new IllegalArgumentException("must contain three parts separated by commas");
        }

        return new TypefaceVariantSize(
                Typeface.parse(parts[0].trim()),
                FontVariant.parse(parts[1].trim()),
                SpaceUnit.PT.fromSp(SpaceUnit.parseDistance(parts[2].trim())));
    }
}

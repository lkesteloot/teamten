// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

/**
 * Represents an immutable ARGB color.
 */
public class Color {
    public static final Color BLACK = new Color(1, 0, 0, 0);
    public static final Color WHITE = new Color(1, 1, 1, 1);

    // In order ARGB, values 0.0 to 1.0.
    private final double[] mValues = new double[4];

    public Color(double alpha, double red, double green, double blue) {
        mValues[0] = alpha;
        mValues[1] = red;
        mValues[2] = green;
        mValues[3] = blue;
    }

    /**
     * Returns the pair-wise sum of the colors. Alpha is unaffected by other's alpha.
     */
    public Color add(Color other) {
        return new Color(mValues[0],
                mValues[1] + other.mValues[1],
                mValues[2] + other.mValues[2],
                mValues[3] + other.mValues[3]);
    }

    /**
     * Returns the pair-wise product of the colors (including alpha).
     */
    public Color multiply(Color other) {
        return new Color(
                mValues[0] * other.mValues[0],
                mValues[1] * other.mValues[1],
                mValues[2] * other.mValues[2],
                mValues[3] * other.mValues[3]);
    }

    /**
     * Returns the color multiplied by a scalar. Only the color components are multiplied,
     * not the transparency.
     */
    public Color multiply(double scalar) {
        return new Color(mValues[0], mValues[1]*scalar, mValues[2]*scalar, mValues[3]*scalar);
    }

    /**
     * Clamp all elements to 0 and 1.
     */
    public Color clamp() {
        return new Color(
                clampScalar(mValues[0]),
                clampScalar(mValues[1]),
                clampScalar(mValues[2]),
                clampScalar(mValues[3]));
    }

    /**
     * Returns a version of the scalar clamped to 0 and 1.
     */
    private double clampScalar(double scalar) {
        if (scalar < 0) {
            return 0;
        }
        if (scalar > 1) {
            return 1;
        }
        return scalar;
    }

    /**
     * Return the packed 32-bit ARGB representation.
     */
    public int toArgb() {
        int alpha = (int) (mValues[0] * 255);
        int red = (int) (mValues[1] * 255);
        int green = (int) (mValues[2] * 255);
        int blue = (int) (mValues[3] * 255);

        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    @Override // Object
    public String toString() {
        return "<" + mValues[0] + "," + mValues[1] + "," + mValues[2] + "," + mValues[3] + ">";
    }
}

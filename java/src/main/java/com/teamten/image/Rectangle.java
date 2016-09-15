// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

/**
 * Immutable integer 2D rectangle.
 */
public class Rectangle {
    private final int mX;
    private final int mY;
    private final int mWidth;
    private final int mHeight;

    /**
     * We keep the constructor private so that there's no confusion over what these
     * four parameters mean.
     */
    private Rectangle(int x, int y, int width, int height) {
        mX = x;
        mY = y;
        mWidth = width;
        mHeight = height;
    }

    /**
     * Create a new instance from a location (inclusive) and a size.
     */
    public static Rectangle makeFromSize(int x, int y, int width, int height) {
        return new Rectangle(x, y, width, height);
    }

    /**
     * Create a new instance from an upper-left location (inclusive) and lower-right
     * location (inclusive).
     */
    public static Rectangle makeFromInclusive(int x1, int y1, int x2, int y2) {
        return new Rectangle(x1, y1, x2 - x1 + 1, y2 - y1 + 1);
    }

    /**
     * Create a new instance from an upper-left location (inclusive) and lower-right
     * location (exclusive).
     */
    public static Rectangle makeFromExclusive(int x1, int y1, int x2, int y2) {
        return new Rectangle(x1, y1, x2 - x1, y2 - y1);
    }

    /**
     * Return X coordinate of upper-left corner, inclusive.
     */
    public int getX() {
        return mX;
    }

    /**
     * Return Y coordinate of upper-left corner, inclusive.
     */
    public int getY() {
        return mY;
    }

    /**
     * Return width of rectangle.
     */
    public int getWidth() {
        return mWidth;
    }

    /**
     * Return height of rectangle.
     */
    public int getHeight() {
        return mHeight;
    }

    /**
     * Return the inclusive lower-right X coordinate.
     */
    public int getInclusiveX2() {
        return mX + mWidth - 1;
    }

    /**
     * Return the inclusive lower-right Y coordinate.
     */
    public int getInclusiveY2() {
        return mY + mHeight - 1;
    }

    /**
     * Return the exclusive lower-right X coordinate.
     */
    public int getExclusiveX2() {
        return mX + mWidth;
    }

    /**
     * Return the exclusive lower-right Y coordinate.
     */
    public int getExclusiveY2() {
        return mY + mHeight;
    }

    /**
     * Return an X11-style string representation of the rectangle in the format
     * "WIDTHxHEIGHT+X+Y".
     */
    public String toString() {
        return mWidth + "x" + mHeight + "+" + mX + "+" + mY;
    }
}


// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Color;
import java.awt.Graphics;

/**
 * The floor that our character stands on.
 */
public class Floor {
    public static final int HEIGHT = 10;

    // Left coordinate, inclusive.
    private final int mLeft;
    // Width of floor.
    private final int mWidth;
    // Top coordinate, inclusive.
    private final int mTop;

    public Floor(int left, int width, int top) {
        mLeft = left;
        mWidth = width;
        mTop = top;
    }

    public void draw(Graphics g) {
        g.setColor(Color.BLUE);
        g.fillRect(mLeft, mTop, mWidth, HEIGHT);
    }
}

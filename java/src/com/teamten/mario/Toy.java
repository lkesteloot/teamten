// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Color;
import java.awt.Graphics;

/**
 * A toy that can be picked up by the ball.
 */
public class Toy {
    private static final Color COLOR = new Color(220, 50, 50);

    // X location of center.
    private final int mX;
    // Y location of center.
    private final int mY;

    // The radius of the toy.
    private final int mRadius;

    public Toy(int x, int y, int radius) {
        mX = x;
        mY = y;
        mRadius = radius;
    }

    public void draw(Graphics g) {
        g.setColor(COLOR);
        g.fillArc(mX - mRadius, mY - mRadius, mRadius*2, mRadius*2, 0, 360);
    }
}


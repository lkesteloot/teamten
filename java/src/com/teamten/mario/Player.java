// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Color;
import java.awt.Graphics;

/**
 * The player, its location, velocity, etc.
 */
public class Player {
    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;

    // X location of top-left corner.
    private final int mX;
    // Y location of top-left corner;
    private final int mY;

    public Player(int x, int y) {
        mX = x;
        mY = y;
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(mX, mY, WIDTH, HEIGHT);
    }
}

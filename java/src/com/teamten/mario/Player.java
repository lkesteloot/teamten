// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Color;
import java.awt.Graphics;

/**
 * The player, its location, velocity, etc.
 */
public class Player {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 10;

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

    public Player move(int dx, int dy) {
        return new Player(mX + dx, mY + dy);
    }

    public void draw(Graphics g) {
        g.setColor(Color.GREEN);
        g.fillRect(mX, mY, WIDTH, HEIGHT);
    }

    @Override // Object
    public int hashCode() {
        int hashCode = 17;

        hashCode = hashCode*31 + getX();
        hashCode = hashCode*31 + getY();

        return hashCode;
    }

    @Override // Object
    public boolean equals(Object other) {
        if (!(other instanceof Player)) {
            return false;
        }

        Player otherPlayer = (Player) other;

        return getX() == otherPlayer.getX()
            && getY() == otherPlayer.getY();
    }
}

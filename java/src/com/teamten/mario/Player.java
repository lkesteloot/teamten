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
    public static final int FRICTION = 4;

    // X location of top-left corner.
    private final int mX;
    // Y location of top-left corner;
    private final int mY;

    // Velocity times VELOCITY_SCALE.
    private final int mVx;
    private final int mVy;
    private static final int VELOCITY_SCALE = 10;

    private Player(int x, int y, int vx, int vy) {
        mX = x;
        mY = y;
        mVx = vx;
        mVy = vy;
    }

    public Player(int x, int y) {
        this(x, y, 0, 0);
    }

    public int getX() {
        return mX;
    }

    public int getY() {
        return mY;
    }

    public double getSpeed() {
        double vx = (double) mVx / VELOCITY_SCALE;
        double vy = (double) mVy / VELOCITY_SCALE;

        return Math.hypot(vx, vy);
    }

    public double distanceTo(Player other) {
        int dx = other.getX() - getX();
        int dy = other.getY() - getY();

        return Math.hypot(dx, dy);
    }

    public Player move(Input input, Env env) {
        int ax = 0;
        int ay = 0;

        if (input.isLeftPressed()) {
            ax -= 1;
        }
        if (input.isRightPressed()) {
            ax += 1;
        }

        int vx = mVx + ax*VELOCITY_SCALE;
        int vy = mVy + ay*VELOCITY_SCALE;
        vx -= Integer.signum(vx)*FRICTION;
        vy -= Integer.signum(vy)*FRICTION;

        return new Player(
                mX + (int) Math.round((double) mVx/VELOCITY_SCALE),
                mY + (int) Math.round((double) mVy/VELOCITY_SCALE),
                vx, vy);
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
        hashCode = hashCode*31 + mVx;
        hashCode = hashCode*31 + mVy;

        return hashCode;
    }

    @Override // Object
    public boolean equals(Object other) {
        if (!(other instanceof Player)) {
            return false;
        }

        Player otherPlayer = (Player) other;

        return getX() == otherPlayer.getX()
            && getY() == otherPlayer.getY()
            && mVx == otherPlayer.mVx
            && mVy == otherPlayer.mVy;
    }
}

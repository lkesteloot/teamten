// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

/**
 * The player, its location, velocity, etc.
 */
public class Player {
    public static final int WIDTH = 10;
    public static final int HEIGHT = 10;
    public static final int FRICTION = 4;
    public static final int GRAVITY = 8;
    public static final int JUMP = 6;

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

    public Point getPoint() {
        return new Point(mX, mY);
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
        boolean touchingFloor = env.isTouchingFloor(this);
        int ax = 0;
        int ay = 0;
        if (touchingFloor) {
            if (input.isJumpPressed()) {
                ay -= JUMP;
            }
            if (input.isLeftPressed()) {
                ax -= 1;
            }
            if (input.isRightPressed()) {
                ax += 1;
            }
        }

        int vx = mVx + ax*VELOCITY_SCALE;
        int vy = mVy + ay*VELOCITY_SCALE;

        if (touchingFloor) {
            vx -= Integer.signum(vx)*FRICTION;
        } else {
            vy += GRAVITY;
        }

        // Move player by its velocity.
        int x = mX + (int) Math.round((double) vx/VELOCITY_SCALE);
        int y = mY + (int) Math.round((double) vy/VELOCITY_SCALE);

        Integer pushBack = env.getPushBack(this, x, y, vx, vy);
        if (pushBack != null) {
            int dy = pushBack.intValue();
            y -= dy;
            if ((vy > 0) == (dy > 0)) {
                vy = 0;
            }
        }

        /*
        System.out.printf("(%d,%d,%d,%d) -> (%d,%d) -> (%d,%d,%d,%d)%n",
                mX, mY, mVx, mVy, ax, ay, x, y, vx, vy);
        */

        // Check if we died.
        if (y > Env.HEIGHT) {
            x = Env.WIDTH/2;
            y = Env.HEIGHT*1/3;
            vx = 0;
            vy = 0;
        }

        return new Player(x, y, vx, vy);
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

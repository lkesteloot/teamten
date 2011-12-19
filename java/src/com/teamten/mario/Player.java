// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;

/**
 * The player, its location, velocity, etc.
 */
public class Player {
    private static final Color COLOR1 = new Color(50, 220, 50);
    private static final Color COLOR2 = new Color(255, 255, 255);
    public static final int INITIAL_RADIUS = 2;
    public static final int FRICTION = 4;
    public static final int GRAVITY = 8;
    public static final int JUMP = 6;

    // X location of top-left corner.
    private final int mX;
    // Y location of top-left corner;
    private final int mY;

    // Rotation of ball in degrees.
    private final int mAngle;

    // The radius of the ball.
    public final double mRadius;

    // Velocity times VELOCITY_SCALE.
    private final int mVx;
    private final int mVy;
    private static final int VELOCITY_SCALE = 10;

    private Player(int x, int y, int angle, int vx, int vy, double radius) {
        mX = x;
        mY = y;
        mAngle = angle;
        mVx = vx;
        mVy = vy;
        mRadius = radius;
    }

    public Player(int x, int y) {
        this(x, y, 0, 0, 0, INITIAL_RADIUS);
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

    public int getRadius() {
        return (int) Math.round(mRadius);
    }

    public int getCircumference() {
        return (int) Math.round(getRadius()*2*Math.PI);
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
        int dx = (int) Math.round((double) vx/VELOCITY_SCALE);
        int x = mX + dx;
        int y = mY + (int) Math.round((double) vy/VELOCITY_SCALE);

        // Roll the ball.
        int angle = mAngle - 360*dx/getCircumference();

        // Increase radius if we rolled.
        double radius = mRadius + Math.abs(dx)/100.0/mRadius;

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
            y = Env.HEIGHT/3;
            vx = 0;
            vy = 0;
            radius = INITIAL_RADIUS;
        }

        return new Player(x, y, angle, vx, vy, radius);
    }

    public void draw(Graphics g) {
        int radius = getRadius();

        g.setColor(COLOR1);
        g.fillArc(mX - radius, mY - radius, radius*2, radius*2, mAngle, 90);
        g.fillArc(mX - radius, mY - radius, radius*2, radius*2, mAngle + 180, 90);
        g.setColor(COLOR2);
        g.fillArc(mX - radius, mY - radius, radius*2, radius*2, mAngle + 90, 90);
        g.fillArc(mX - radius, mY - radius, radius*2, radius*2, mAngle + 270, 90);
    }

    @Override // Object
    public int hashCode() {
        int hashCode = 17;

        hashCode = hashCode*31 + getX();
        hashCode = hashCode*31 + getY();
        hashCode = hashCode*31 + mVx;
        hashCode = hashCode*31 + mVy;
        hashCode = hashCode*31 + getRadius();

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
            && mVy == otherPlayer.mVy
            && getRadius() == otherPlayer.getRadius();
    }
}

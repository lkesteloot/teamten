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
    public static final int JUMP = 6;

    // X location of center.
    private final int mX;
    // Y location of center.
    private final int mY;

    // Rotation of ball in degrees.
    private final int mAngle;

    // The radius of the ball.
    private final double mRadius;

    // Velocity times VELOCITY_SCALE.
    private final int mVx;
    private final int mVy;
    public static final int VELOCITY_SCALE = 10;

    public Player(int x, int y, int angle, int vx, int vy, double radius) {
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

    public int getAngle() {
        return mAngle;
    }

    public int getVx() {
        return mVx;
    }

    public int getVy() {
        return mVy;
    }

    public Point getPoint() {
        return new Point(mX, mY);
    }

    public double getRealRadius() {
        return mRadius;
    }

    public int getSnappedRadius() {
        return (int) Math.round(mRadius);
    }

    public int getCircumference() {
        return (int) Math.round(getSnappedRadius()*2*Math.PI);
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

    public void draw(Graphics g) {
        int radius = getSnappedRadius();

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
        hashCode = hashCode*31 + getSnappedRadius();

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
            && getSnappedRadius() == otherPlayer.getSnappedRadius();
    }
}

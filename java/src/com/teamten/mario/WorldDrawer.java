// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Canvas;
import java.awt.geom.Path2D;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Point;

import java.util.List;
import java.util.Collections;

/**
 * Draws a World.
 */
public class WorldDrawer extends Canvas {
    private volatile World mWorld;
    private double mScale = 1;
    private double mTx = 0;
    private double mTy = 0;
    private List<Point> mPath = Collections.emptyList();
    private List<Point> mExplored = Collections.emptyList();
    private Point mTarget = null;

    public WorldDrawer(World world) {
        mWorld = world;
    }

    public void setWorld(World world) {
        mWorld = world;
        repaint();
    }

    /**
     * Debug path to draw.
     */
    public void setPath(List<Point> path) {
        mPath = path;
        repaint();
    }

    /**
     * Set of points we explored.
     */
    public void setExplored(List<Point> explored) {
        mExplored = explored;
        repaint();
    }

    public void setTarget(Point target) {
        mTarget = target;
        repaint();
    }

    public void reverseTransform(Point point) {
        point.setLocation((point.x - mTx)/mScale, (point.y - mTy)/mScale);
    }

    @Override // Canvas
    public void paint(Graphics g) {
        // Grab the world here so we can use it throughout the draw routine in
        // case it gets changed.
        World world = mWorld;

        // Use fancier graphics.
        Graphics2D g2 = (Graphics2D) g;

        // Get window dimensions.
        Dimension dimension = getSize();
        int width = dimension.width;
        int height = dimension.height;

        // Scale graphics to window size.
        if (width*Env.HEIGHT > Env.WIDTH*height) {
            // Window wider than world.
            mScale = (double) height/Env.HEIGHT;
            mTx = (width - Env.WIDTH*mScale)/2.0;
            mTy = 0;
        } else {
            // Window taller than world.
            mScale = (double) width/Env.WIDTH;
            mTx = 0;
            mTy = (height - Env.HEIGHT*mScale)/2.0;
        }
        g2.translate(mTx, mTy);
        g2.scale(mScale, mScale);

        // Draw the world.
        world.draw(g);

        // Draw points we explored.
        g2.setColor(Color.BLACK);
        for (Point point : mExplored) {
            // g2.drawRect(point.x, point.y, 0, 0);
        }

        // Draw debug path.
        Path2D.Double debugShape = new Path2D.Double();
        boolean first = true;
        for (Point point : mPath) {
            if (first) {
                debugShape.moveTo(point.x, point.y);
                first = false;
            } else {
                debugShape.lineTo(point.x, point.y);
            }
        }
        g2.setColor(Color.RED);
        g2.draw(debugShape);

        // Draw target.
        if (mTarget != null) {
            g2.setColor(Color.WHITE);
            g2.drawArc(mTarget.x - 2, mTarget.y - 2, 4, 4, 0, 360);
        }
    }
}

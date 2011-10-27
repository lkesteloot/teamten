// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.Point;

/**
 * Draws a World.
 */
public class WorldDrawer extends Canvas {
    private volatile World mWorld;
    private double mScale = 1;
    private double mTx = 0;
    private double mTy = 0;

    public WorldDrawer(World world) {
        mWorld = world;
    }

    public void setWorld(World world) {
        mWorld = world;
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
    }
}

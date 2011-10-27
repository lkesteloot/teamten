// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;

/**
 * Draws a World.
 */
public class WorldDrawer extends Canvas {
    private volatile World mWorld;

    public WorldDrawer(World world) {
        mWorld = world;
    }

    public void setWorld(World world) {
        mWorld = world;
        repaint();
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
        double scale;
        double tx;
        double ty;
        if (width*Env.HEIGHT > Env.WIDTH*height) {
            // Window wider than world.
            scale = (double) height/Env.HEIGHT;
            tx = (width - Env.WIDTH*scale)/2.0;
            ty = 0;
        } else {
            // Window taller than world.
            scale = (double) width/Env.WIDTH;
            tx = 0;
            ty = (height - Env.HEIGHT*scale)/2.0;
        }
        g2.translate(tx, ty);
        g2.scale(scale, scale);

        // Draw the world.
        world.draw(g);
    }
}

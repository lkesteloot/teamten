// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Canvas;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

/**
 * Draws a World.
 */
public class WorldDrawer extends Canvas implements KeyListener {
    private World mWorld;

    public WorldDrawer(World world) {
        mWorld = world;

        addKeyListener(this);
    }

    @Override // Canvas
    public void paint(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        Dimension dimension = getSize();

        int width = dimension.width;
        int height = dimension.height;

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

        mWorld.draw(g);
    }

    @Override // KeyListener
    public void keyPressed(KeyEvent keyEvent) {
    }

    @Override // KeyListener
    public void keyReleased(KeyEvent keyEvent) {
    }

    @Override // KeyListener
    public void keyTyped(KeyEvent keyEvent) {
    }
}


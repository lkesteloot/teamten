// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.BorderLayout;
import java.awt.Component;

import javax.swing.JFrame;
import javax.swing.JPanel;

/**
 * Side-scroller that uses AI to get to its goal.
 */
public class Mario extends JFrame {
    public static void main(String[] args) {
        new Mario();
    }

    private Mario() {
        Env env = Env.makeEnv();
        Player player = new Player(Env.WIDTH/2, Env.HEIGHT/2);
        World world = new World(env, player);
        WorldDrawer worldDrawer = new WorldDrawer(world);

        makeUi(worldDrawer);
    }

    private void makeUi(WorldDrawer worldDrawer) {
        JPanel content = (JPanel) getContentPane();
        content.setLayout(new BorderLayout());
        content.add(worldDrawer, BorderLayout.CENTER);

        setSize(Env.WIDTH*4, Env.HEIGHT*4);
        setVisible(true);
    }
}

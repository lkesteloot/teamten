// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Graphics;

/**
 * The environment and the characters within it.
 */
public class World {
    private final Env mEnv;
    private final Player mPlayer;

    public World(Env env, Player player) {
        mEnv = env;
        mPlayer = player;
    }

    public void draw(Graphics g) {
        mEnv.draw(g);
        mPlayer.draw(g);
    }
}


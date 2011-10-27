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

    public World step(Input input) {
        int dx = 0;
        int dy = 0;

        if (input.isLeftPressed()) {
            dx -= 1;
        }
        if (input.isRightPressed()) {
            dx += 1;
        }

        return new World(mEnv, mPlayer.move(dx, dy));
    }
}

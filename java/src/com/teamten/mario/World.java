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
        getEnv().draw(g);
        getPlayer().draw(g);
    }

    public Env getEnv() {
        return mEnv;
    }

    public Player getPlayer() {
        return mPlayer;
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

        return new World(getEnv(), getPlayer().move(dx, dy));
    }

    @Override // Object
    public int hashCode() {
        return getEnv().hashCode() + 31*getPlayer().hashCode();
    }

    @Override // Object
    public boolean equals(Object other) {
        if (!(other instanceof World)) {
            return false;
        }

        World otherWorld = (World) other;

        return getEnv().equals(otherWorld.getEnv())
            && getPlayer().equals(otherWorld.getPlayer());
    }
}

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
        return new World(getEnv(), getPlayer().move(input, getEnv()));
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

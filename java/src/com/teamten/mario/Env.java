// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.util.List;
import java.util.ArrayList;
import java.awt.Graphics;
import java.awt.Color;

/**
 * The environment (walls, etc.) that our character lives in.
 */
public class Env {
    public static final int WIDTH = 200;
    public static final int HEIGHT = 100;
    private final List<Floor> mFloorList = new ArrayList<Floor>();

    public static Env makeEnv() {
        Env env = new Env();

        env.addFloor(new Floor(0, WIDTH, HEIGHT - Floor.HEIGHT));

        return env;
    }

    private void addFloor(Floor floor) {
        mFloorList.add(floor);
    }

    public boolean isTouchingFloor(Player player) {
        int floor = HEIGHT - Floor.HEIGHT;
        return player.getY() + Player.HEIGHT >= floor;
    }

    public Integer getPushBack(Player player, int x, int y) {
        int floor = HEIGHT - Floor.HEIGHT;
        int dy = y + Player.HEIGHT - floor;
        if (dy >= 0) {
            return dy;
        } else {
            return null;
        }
    }

    public void draw(Graphics g) {
        g.setColor(Color.GRAY);
        g.fillRect(0, 0, WIDTH, HEIGHT);

        for (Floor floor : mFloorList) {
            floor.draw(g);
        }
    }
}

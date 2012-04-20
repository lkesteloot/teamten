// Copyright 2012 Lawrence Kesteloot

package com.teamten.mario;

import java.util.Random;

/**
 * Generate the various levels.
 */
public class Levels {
    public static final int LEVEL_COUNT = 2;

    public static Env makeLevel(int levelNumber) {
        switch (levelNumber) {
            case 0:
                return makeLevel0();

            case 1:
                return makeLevel1();

            default:
                throw new IllegalArgumentException("Invalid level " + levelNumber);
        }
    }

    private static Env makeLevel0() {
        Env env = new Env();

        env.addFloor(new Floor(0, Env.WIDTH, Env.HEIGHT - Floor.HEIGHT));
        env.addFloor(new Floor(Env.WIDTH/5*1, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*4));
        env.addFloor(new Floor(Env.WIDTH/5*2, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*7));
        env.addFloor(new Floor(Env.WIDTH/5*3, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*10));

        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            int floorIndex = random.nextInt(env.getFloorCount());
            Floor floor = env.getFloor(floorIndex);

            int x = floor.getLeft() + random.nextInt(floor.getWidth());
            int r = (int) (3*Math.pow(Math.random(), 4)) + 1;
            env.addToy(new Toy(x, floor.getTop() - r, r));
        }

        return env;
    }

    private static Env makeLevel1() {
        Env env = new Env();

        env.addFloor(new Floor(0, Env.WIDTH, Env.HEIGHT - Floor.HEIGHT));
        env.addFloor(new Floor(Env.WIDTH/5*1, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*10));
        env.addFloor(new Floor(Env.WIDTH/5*2, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*7));
        env.addFloor(new Floor(Env.WIDTH/5*3, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*4));

        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            int floorIndex = random.nextInt(env.getFloorCount());
            Floor floor = env.getFloor(floorIndex);

            int x = floor.getLeft() + random.nextInt(floor.getWidth());
            int r = (int) (3*Math.pow(Math.random(), 4)) + 1;
            env.addToy(new Toy(x, floor.getTop() - r, r));
        }

        return env;
    }
}

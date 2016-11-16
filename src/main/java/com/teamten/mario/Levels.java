/*
 *
 *    Copyright 2016 Lawrence Kesteloot
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

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

        return env;
    }

    private static Env makeLevel1() {
        Env env = new Env();

        env.addFloor(new Floor(0, Env.WIDTH, Env.HEIGHT - Floor.HEIGHT));
        env.addFloor(new Floor(Env.WIDTH/5*1, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*10));
        env.addFloor(new Floor(Env.WIDTH/5*2, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*7));
        env.addFloor(new Floor(Env.WIDTH/5*3, Env.WIDTH/5, Env.HEIGHT - Floor.HEIGHT*4));

        addToys(env);

        return env;
    }

    /**
     * Add toys to a level.
     */
    private static void addToys(Env env) {
        Random random = new Random();
        for (int i = 0; i < 30; i++) {
            int floorIndex = random.nextInt(env.getFloorCount());
            Floor floor = env.getFloor(floorIndex);

            int x = floor.getLeft() + random.nextInt(floor.getWidth());
            int r = (int) (3*Math.pow(Math.random(), 4)) + 1;
            env.addToy(new Toy(x, floor.getTop() - r, r));
        }
    }
}

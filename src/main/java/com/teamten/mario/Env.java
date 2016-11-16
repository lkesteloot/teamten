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

// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Color;
import java.awt.Graphics;

import java.util.ArrayList;
import java.util.List;

/**
 * The environment (walls, toys, etc.) that our character lives in.
 */
public class Env {
    private static final int DARK_BLUE = 50;
    private static final int LIGHT_BLUE = 150;
    public static final int WIDTH = 200;
    public static final int HEIGHT = 100;
    private final List<Floor> mFloorList = new ArrayList<Floor>();
    private final List<Toy> mToyList = new ArrayList<Toy>();

    public void addFloor(Floor floor) {
        mFloorList.add(floor);
    }

    public int getFloorCount() {
        return mFloorList.size();
    }

    public Floor getFloor(int index) {
        return mFloorList.get(index);
    }

    public void addToy(Toy toy) {
        mToyList.add(toy);
    }

    public Toy getToy(int index) {
        return mToyList.get(index);
    }

    public int getToyIndex(Player player) {
        // XXX Inefficient. Use space partitioning tree.
        for (int i = 0; i < mToyList.size(); i++) {
            if (mToyList.get(i).isOnPlayer(player)) {
                return i;
            }
        }

        return -1;
    }

    public Env withoutToy(int index) {
        Env env = new Env();

        // XXX Inefficient.
        env.mFloorList.addAll(mFloorList);
        for (int i = 0; i < mToyList.size(); i++) {
            if (i != index) {
                env.addToy(mToyList.get(i));
            }
        }

        return env;
    }

    public boolean isTouchingFloor(Player player) {
        int playerBottom = player.getY() + player.getSnappedRadius() - 1;

        for (Floor floor : mFloorList) {
            if (playerFloorHorizontalOverlap(player.getX(), player.getSnappedRadius(), floor)) {
                if (playerBottom == floor.getTop() - 1) {
                    return true;
                }
            }
        }

        return false;
    }

    public Integer getPushBack(Player player, int x, int y, int vx, int vy) {
        for (Floor floor : mFloorList) {
            if (playerFloorHorizontalOverlap(x, player.getSnappedRadius(), floor)) {
                if (playerFloorVerticalOverlap(y, player.getSnappedRadius(), floor)) {
                    if (vy > 0) {
                        // Going down.
                        return y + player.getSnappedRadius() - floor.getTop();
                    } else {
                        return y - player.getSnappedRadius() - (floor.getTop() + Floor.HEIGHT);
                    }
                }
            }
        }

        return null;
    }

    private static boolean playerFloorHorizontalOverlap(int playerX,
            int playerRadius, Floor floor) {

        return playerX + playerRadius - 1 >= floor.getLeft()
            && playerX - playerRadius + 1 < floor.getLeft() + floor.getWidth() - 1;
    }

    private static boolean playerFloorVerticalOverlap(int playerY,
            int playerRadius, Floor floor) {
        return playerY + playerRadius - 1 >= floor.getTop()
            && playerY - playerRadius + 1 < floor.getTop() + Floor.HEIGHT - 1;
    }

    public void draw(Graphics g) {
        // Sky.
        int step = 1;
        for (int y = 0; y < HEIGHT; y += step) {
            int white = DARK_BLUE + y*(LIGHT_BLUE - DARK_BLUE)/(HEIGHT - 1);
            g.setColor(new Color(white*6/10, white, 255));

            // Need to add +1 here because otherwise the anti-aliasing will make it look bad.
            g.fillRect(0, y, WIDTH, step + 1);
        }

        // Floors.
        for (Floor floor : mFloorList) {
            floor.draw(g);
        }

        // Toys.
        for (Toy toy : mToyList) {
            toy.draw(g);
        }
    }
}

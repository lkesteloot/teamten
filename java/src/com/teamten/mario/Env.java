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
    private static final int DARK_BLUE = 50;
    private static final int LIGHT_BLUE = 150;
    public static final int WIDTH = 200;
    public static final int HEIGHT = 100;
    private final List<Floor> mFloorList = new ArrayList<Floor>();
    private final List<Toy> mToyList = new ArrayList<Toy>();

    public static Env makeEnv() {
        Env env = new Env();

        env.addFloor(new Floor(0, WIDTH, HEIGHT - Floor.HEIGHT));
        env.addFloor(new Floor(WIDTH/5, WIDTH/5, HEIGHT - Floor.HEIGHT*4));
        env.addFloor(new Floor(WIDTH/5*2, WIDTH/5, HEIGHT - Floor.HEIGHT*7));

        int toyRadius = 1;
        env.addToy(new Toy(WIDTH/10, HEIGHT - Floor.HEIGHT - toyRadius, toyRadius));

        return env;
    }

    private void addFloor(Floor floor) {
        mFloorList.add(floor);
    }

    private void addToy(Toy toy) {
        mToyList.add(toy);
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

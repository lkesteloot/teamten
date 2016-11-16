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

/**
 * A toy that can be picked up by the ball.
 */
public class Toy {
    private static final Color COLOR = new Color(220, 50, 50);

    // X location of center.
    private final int mX;
    // Y location of center.
    private final int mY;

    // The radius of the toy.
    private final int mRadius;

    public Toy(int x, int y, int radius) {
        mX = x;
        mY = y;
        mRadius = radius;
    }

    public int getRadius() {
        return mRadius;
    }

    public boolean isOnPlayer(Player player) {
        // Check that toy isn't too big for player.
        int pr = player.getSnappedRadius();
        if (mRadius*2 > pr) {
            return false;
        }

        int px = player.getX();
        int py = player.getY();

        long dx = px - mX;
        long dy = py - mY;
        long dr = pr - mRadius;
        long distSquared = dx*dx + dy*dy;
        long radiusSquared = dr*dr;

        return distSquared <= radiusSquared + 1;
    }

    public void draw(Graphics g) {
        g.setColor(COLOR);
        g.fillArc(mX - mRadius, mY - mRadius, mRadius*2, mRadius*2, 0, 360);
    }
}


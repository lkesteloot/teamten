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
 * The floor that our character stands on.
 */
public class Floor {
    private static final Color COLOR = new Color(100, 60, 0);
    public static final int HEIGHT = 8;

    // Left coordinate, inclusive.
    private final int mLeft;
    // Width of floor.
    private final int mWidth;
    // Top coordinate, inclusive.
    private final int mTop;

    public Floor(int left, int width, int top) {
        mLeft = left;
        mWidth = width;
        mTop = top;
    }

    public int getLeft() {
        return mLeft;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getTop() {
        return mTop;
    }

    public void draw(Graphics g) {
        g.setColor(COLOR);
        g.fillRect(mLeft, mTop, mWidth, HEIGHT);
    }
}

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

import java.awt.Graphics;

/**
 * The environment and the characters within it.
 */
public class World {
    public static final int GRAVITY = 8;
    public static final int FRICTION = 4;
    private static final boolean IS_PERSON = true;
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
        Env env = getEnv();
        Player player = getPlayer();

        boolean touchingFloor = env.isTouchingFloor(player);
        int ax = 0;
        int ay = 0;
        if (touchingFloor) {
            if (input.isJumpPressed()) {
                ay -= Player.JUMP;
            }
            if (input.isLeftPressed()) {
                ax -= 1;
            }
            if (input.isRightPressed()) {
                ax += 1;
            }
        }

        // Compute new velocity.
        int vx;
        int vy = player.getVy() + ay*Player.VELOCITY_SCALE;

        if (IS_PERSON) {
            if (touchingFloor) {
                vx = 3*ax*Player.VELOCITY_SCALE;
            } else {
                vx = player.getVx();
            }
        } else {
            vx = player.getVx() + ax*Player.VELOCITY_SCALE;
        }

        // Friction and gravity.
        if (touchingFloor) {
            if (!IS_PERSON) {
                vx -= Integer.signum(vx)*FRICTION;
            }
        } else {
            vy += GRAVITY;
        }

        // Move player by its velocity.
        int dx = (int) Math.round((double) vx/Player.VELOCITY_SCALE);
        int x = player.getX() + dx;
        int y = player.getY() + (int) Math.round((double) vy/Player.VELOCITY_SCALE);

        // Roll the ball.
        int angle = player.getAngle() - 360*dx/player.getCircumference();

        // Increase radius if we're on toy.
        double radius = player.getRealRadius();
        int toyIndex = env.getToyIndex(player);
        if (toyIndex >= 0) {
            // Increase area.
            Toy toy = env.getToy(toyIndex);
            radius += 2.0*toy.getRadius()*toy.getRadius()/(radius*radius);
            env = env.withoutToy(toyIndex);
        }

        // Push off the walls and floors.
        Integer pushBack = env.getPushBack(player, x, y, vx, vy);
        if (pushBack != null) {
            int dy = pushBack.intValue();
            y -= dy;
            if ((vy > 0) == (dy > 0)) {
                vy = 0;
            }
        }

        /*
        System.out.printf("(%d,%d,%d,%d) -> (%d,%d) -> (%d,%d,%d,%d)%n",
                mX, mY, player.getVx(), mVy, ax, ay, x, y, vx, vy);
        */

        // Check if we died.
        if (y > Env.HEIGHT) {
            x = Env.WIDTH/2;
            y = Env.HEIGHT/3;
            vx = 0;
            vy = 0;
            // radius = Player.INITIAL_RADIUS;
        }

        Player newPlayer = new Player(x, y, angle, vx, vy, radius);

        return new World(env, newPlayer);
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

// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

import java.awt.Graphics;

/**
 * The environment and the characters within it.
 */
public class World {
    public static final int GRAVITY = 8;
    public static final int FRICTION = 4;
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

        int vx = player.getVx() + ax*Player.VELOCITY_SCALE;
        int vy = player.getVy() + ay*Player.VELOCITY_SCALE;

        if (touchingFloor) {
            vx -= Integer.signum(vx)*FRICTION;
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
            radius += 1.0/(radius*radius);
            env = env.withoutToy(toyIndex);
        }

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
            radius = Player.INITIAL_RADIUS;
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

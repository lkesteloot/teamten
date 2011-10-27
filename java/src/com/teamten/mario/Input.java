// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

/**
 * Records the state of the input keys.
 */
public class Input {
    public static final Input NOTHING = new Input(false, false, false);
    public static final Input JUMP = new Input(true, false, false);
    public static final Input LEFT = new Input(false, true, false);
    public static final Input RIGHT = new Input(false, false, true);
    private final boolean mJumpPressed;
    private final boolean mLeftPressed;
    private final boolean mRightPressed;

    private Input(boolean jumpPressed, boolean leftPressed, boolean rightPressed) {
        mJumpPressed = jumpPressed;
        mLeftPressed = leftPressed;
        mRightPressed = rightPressed;
    }

    public boolean isJumpPressed() {
        return mJumpPressed;
    }

    public boolean isLeftPressed() {
        return mLeftPressed;
    }

    public boolean isRightPressed() {
        return mRightPressed;
    }

    public Input withJumpPressed(boolean jumpPressed) {
        return new Input(jumpPressed, mLeftPressed, mRightPressed);
    }

    public Input withLeftPressed(boolean leftPressed) {
        return new Input(mJumpPressed, leftPressed, mRightPressed);
    }

    public Input withRightPressed(boolean rightPressed) {
        return new Input(mJumpPressed, mLeftPressed, rightPressed);
    }

    @Override // Object
    public String toString() {
        String s = (isJumpPressed() ? "J" : "")
            + (isLeftPressed() ? "L" : "")
            + (isRightPressed() ? "R" : "");
        if (s.isEmpty()) {
            s = "_";
        }
        return s;
    }
}

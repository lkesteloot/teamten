// Copyright 2011 Lawrence Kesteloot

package com.teamten.mario;

/**
 * Records the state of the input keys.
 */
public class Input {
    private final boolean mJumpPressed;
    private final boolean mLeftPressed;
    private final boolean mRightPressed;

    public Input() {
        this(false, false, false);
    }

    public Input(boolean jumpPressed, boolean leftPressed, boolean rightPressed) {
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
}

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

/**
 * Records the state of the input keys.
 */
public class Input {
    public static final Input NOTHING = new Input(false, false, false);
    public static final Input JUMP = new Input(true, false, false);
    public static final Input LEFT = new Input(false, true, false);
    public static final Input RIGHT = new Input(false, false, true);
    public static final Input LEFT_JUMP = new Input(true, true, false);
    public static final Input RIGHT_JUMP = new Input(true, false, true);
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

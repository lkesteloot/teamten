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

package com.teamten.chess;

/**
 * Thrown when a user tries to make a move that's not valid or legal.
 */
public class IllegalMoveException extends Exception {
    /**
     * Location of the piece that would be putting the king in check.
     */
    private final int mCheckIndex;

    public IllegalMoveException(String message) {
        this(message, -1);
    }

    public IllegalMoveException(String message, int checkIndex) {
        super(message);
        mCheckIndex = checkIndex;
    }

    /**
     * If this move is illegal because the king would be in check, returns
     * one of pieces that would be putting it in check. Otherwise returns -1.
     */
    public int getCheckIndex() {
        return mCheckIndex;
    }
}


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

package com.teamten.typeset;

/**
 * Simple class that implements the Dimensions interface.
 */
public class AbstractDimensions implements Dimensions {
    private final long mWidth;
    private final long mHeight;
    private final long mDepth;

    public AbstractDimensions(long width, long height, long depth) {
        mWidth = width;
        mHeight = height;
        mDepth = depth;
    }

    @Override
    public long getWidth() {
        return mWidth;
    }

    @Override
    public long getHeight() {
        return mHeight;
    }

    @Override
    public long getDepth() {
        return mDepth;
    }
}

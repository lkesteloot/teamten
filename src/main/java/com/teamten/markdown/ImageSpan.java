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

package com.teamten.markdown;

/**
 * Stores an image and its caption.
 */
public class ImageSpan extends Span {
    private final String mPathname;
    private final Block mCaption;

    public ImageSpan(String pathname, Block caption) {
        mPathname = pathname;
        mCaption = caption;
    }

    public String getPathname() {
        return mPathname;
    }

    /**
     * The caption as a block, or an empty string if none was specified.
     */
    public Block getCaption() {
        return mCaption;
    }

    @Override
    public String toString() {
        return "[!" + mPathname + " " + mCaption + "]";
    }
}

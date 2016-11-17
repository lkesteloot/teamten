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
 * Stores an image tag.
 */
public class ImageSpan extends Span {
    private final String mPathname;
    private final String mCaption;

    public ImageSpan(String pathname, String caption) {
        mPathname = pathname;
        mCaption = caption;
    }

    /**
     * Creates a {@code ImageSpan} from a tag that consists of a pathname
     * (that doesn't contain any spaces), a space, and a caption.
     */
    public static ImageSpan fromTag(String tag) {
        String[] parts = tag.trim().split(" ", 2);

        if (parts.length == 1) {
            return new ImageSpan(parts[0], "");
        } else {
            return new ImageSpan(parts[0], parts[1].trim());
        }
    }

    public String getPathname() {
        return mPathname;
    }

    /**
     * The caption as a marked-up paragraph, or an empty string if none was specified.
     */
    public String getCaption() {
        return mCaption;
    }
}

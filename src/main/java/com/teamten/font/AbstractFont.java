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

package com.teamten.font;

import com.teamten.font.Font;
import com.teamten.typeset.Ligatures;

/**
 * Parent of real font classes, with stub methods.
 */
public abstract class AbstractFont implements Font {
    private Ligatures mLigatures;

    /**
     * An abstract font with no ligature support.
     */
    public AbstractFont() {
        this(null);
    }

    /**
     * An abstract font with ligature support.
     */
    public AbstractFont(Ligatures ligatures) {
        mLigatures = ligatures;
    }

    protected void setLigatures(Ligatures ligatures) {
        mLigatures = ligatures;
    }

    @Override
    public long getKerning(int leftChar, int rightChar, double fontSize) {
        // No kerning by default.
        return 0;
    }

    @Override
    public String transformLigatures(String text) {
        return mLigatures == null ? text : mLigatures.transform(text);
    }
}

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

import org.jetbrains.annotations.NotNull;

/**
 * Represents both a typeface (e.g., Helvetica) and its variant (e.g., bold italic).
 */
public class TypefaceVariant {
    private final @NotNull Typeface mTypeface;
    private final @NotNull FontVariant mFontVariant;

    public TypefaceVariant(Typeface typeface, FontVariant fontVariant) {
        mTypeface = typeface;
        mFontVariant = fontVariant;
    }

    public Typeface getTypeface() {
        return mTypeface;
    }

    public FontVariant getFontVariant() {
        return mFontVariant;
    }

    /**
     * Utility method that gets the {@link FontName} from the typeface for the font variant.
     */
    public FontName getFontName() {
        return getTypeface().get(getFontVariant());
    }

    @Override
    public String toString() {
        return mTypeface + ", " + mFontVariant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TypefaceVariant that = (TypefaceVariant) o;

        if (mTypeface != that.mTypeface) {
            return false;
        }
        return mFontVariant == that.mFontVariant;

    }

    @Override
    public int hashCode() {
        int result = mTypeface.hashCode();
        result = 31*result + mFontVariant.hashCode();
        return result;
    }
}

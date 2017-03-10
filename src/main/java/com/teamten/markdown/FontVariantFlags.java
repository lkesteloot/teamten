/*
 *
 *    Copyright 2017 Lawrence Kesteloot
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
 * Like the {@link com.teamten.font.FontVariant} class, but as a set of flags that can be built
 * up in a parser. The object is immutable.
 */
public class FontVariantFlags {
    /**
     * Everything off.
     */
    public static final FontVariantFlags PLAIN = new FontVariantFlags(false, false, false, false);
    private final boolean mBold;
    private final boolean mItalic;
    private final boolean mSmallCaps;
    private final boolean mCode;

    private FontVariantFlags(boolean bold, boolean italic, boolean smallCaps, boolean code) {
        mBold = bold;
        mItalic = italic;
        mSmallCaps = smallCaps;
        mCode = code;
    }

    public FontVariantFlags toggleBold() {
        return new FontVariantFlags(!mBold, mItalic, mSmallCaps, mCode);
    }

    public FontVariantFlags toggleItalic() {
        return new FontVariantFlags(mBold, !mItalic, mSmallCaps, mCode);
    }

    public FontVariantFlags withSmallCaps(boolean smallCaps) {
        return new FontVariantFlags(mBold, mItalic, smallCaps, mCode);
    }

    public FontVariantFlags toggleCode() {
        return new FontVariantFlags(mBold, mItalic, mSmallCaps, !mCode);
    }

    public boolean isBold() {
        return mBold;
    }

    public boolean isItalic() {
        return mItalic;
    }

    public boolean isSmallCaps() {
        return mSmallCaps;
    }

    public boolean isCode() {
        return mCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        FontVariantFlags that = (FontVariantFlags) o;

        if (mBold != that.mBold) {
            return false;
        }
        if (mItalic != that.mItalic) {
            return false;
        }
        if (mSmallCaps != that.mSmallCaps) {
            return false;
        }
        return mCode == that.mCode;
    }

    @Override
    public int hashCode() {
        int result = (mBold ? 1 : 0);
        result = 31*result + (mItalic ? 1 : 0);
        result = 31*result + (mSmallCaps ? 1 : 0);
        result = 31*result + (mCode ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FontVariantFlags{" +
                "mBold=" + mBold +
                ", mItalic=" + mItalic +
                ", mSmallCaps=" + mSmallCaps +
                ", mCode=" + mCode +
                '}';
    }
}

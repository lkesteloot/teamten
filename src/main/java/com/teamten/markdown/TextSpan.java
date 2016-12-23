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
 * A text span is a horizontal sequence of letters with a specific set of attributes, like font.
 */
public class TextSpan extends Span {
    private final String mText;
    private final boolean mIsBold;
    private final boolean mIsItalic;
    private final boolean mIsSmallCaps;
    private final boolean mIsCode;

    public TextSpan(String text, boolean isBold, boolean isItalic, boolean isSmallCaps, boolean isCode) {
        mText = text;
        mIsBold = isBold;
        mIsItalic = isItalic;
        mIsSmallCaps = isSmallCaps;
        mIsCode = isCode;
    }

    public String getText() {
        return mText;
    }

    public boolean isBold() {
        return mIsBold;
    }

    public boolean isItalic() {
        return mIsItalic;
    }

    public boolean isSmallCaps() {
        return mIsSmallCaps;
    }

    public boolean isCode() {
        return mIsCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TextSpan textSpan = (TextSpan) o;

        if (mIsBold != textSpan.mIsBold) {
            return false;
        }
        if (mIsItalic != textSpan.mIsItalic) {
            return false;
        }
        if (mIsSmallCaps != textSpan.mIsSmallCaps) {
            return false;
        }
        if (mIsCode != textSpan.mIsCode) {
            return false;
        }
        return mText.equals(textSpan.mText);
    }

    @Override
    public int hashCode() {
        int result = mText.hashCode();
        result = 31*result + (mIsBold ? 1 : 0);
        result = 31*result + (mIsItalic ? 1 : 0);
        result = 31*result + (mIsSmallCaps ? 1 : 0);
        result = 31*result + (mIsCode ? 1 : 0);
        return result;
    }

    @Override
    public String toString() {
        return mText;
    }
}

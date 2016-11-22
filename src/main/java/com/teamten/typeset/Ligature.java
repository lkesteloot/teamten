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
 * Represents a single ligature.
 */
public class Ligature {
    private final String mMulti;
    private final String mSingleString;
    private final int mSingleChar;

    public Ligature(String multi, int singleChar) {
        mMulti = multi;
        int[] codePoints = new int[1];
        codePoints[0] = singleChar;
        mSingleString = new String(codePoints, 0, 1);
        mSingleChar = singleChar;
    }

    /**
     * The multi-character version of the ligature (e.g., "fi").
     */
    public String getMulti() {
        return mMulti;
    }

    /**
     * The single-character version of the ligature (e.g., "ﬁ"), as a string.
     */
    public String getSingleString() {
        return mSingleString;
    }

    /**
     * The single-character version of the ligature (e.g., "ﬁ"), as a code point.
     */
    public int getSingleChar() {
        return mSingleChar;
    }

    /**
     * Replace all instances of the ligature in the string with the single-char version.
     */
    public String replace(String s) {
        return s.replace(getMulti(), getSingleString());
    }
}


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

package com.teamten.typeset.element;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents an amount by which a flexible element can stretch or shrink.
 */
public class Flexibility {
    private final long mAmount;
    private final boolean mIsInfinite;

    public Flexibility(long amount, boolean isInfinite) {
        mAmount = amount;
        mIsInfinite = isInfinite;
    }

    /**
     * Amount by which we can stretch or shrink.
     */
    public long getAmount() {
        return mAmount;
    }

    /**
     * Whether this is an infinite stretch or shrink, meaning that non-infinite ones don't factor.
     */
    public boolean isInfinite() {
        return mIsInfinite;
    }

    public String toString(String prefix) {
        if (mAmount == 0) {
            return "";
        } else {
            return String.format("%s%.1f%s", prefix, PT.fromSp(mAmount), mIsInfinite ? "inf" : "pt");
        }
    }
}

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

/**
 * Keeps track of a sum of flexibility, along with whether the infinite amount trumps the finite one.
 */
public class TotalFlexibility {
    private long mFiniteAmount = 0;
    private long mInfiniteAmount = 0;

    public void add(Flexibility expandability) {
        if (expandability.isInfinite()) {
            mInfiniteAmount += expandability.getAmount();
        } else {
            mFiniteAmount += expandability.getAmount();
        }
    }

    /**
     * Whether there was non-zero infinite expandability.
     */
    public boolean isInfinite() {
        return mInfiniteAmount != 0;
    }

    /**
     * The expandability, where any finite amount trumps the finite one.
     */
    public long getAmount() {
        return isInfinite() ? mInfiniteAmount : mFiniteAmount;
    }

    /**
     * The total expressed as an immutable Flexibility object.
     */
    public Flexibility toFlexibility() {
        return new Flexibility(getAmount(), isInfinite());
    }
}

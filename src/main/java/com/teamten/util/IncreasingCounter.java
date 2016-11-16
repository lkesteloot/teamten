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

package com.teamten.util;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Enumerates all combinations of N digits in base B with the restriction that digits are always
 * in strictly increasing order. For example: 0,1,2, then 0,1,3, then 0,1,4, all the way to 7,8,9 for
 * base 10 and three digits.
 *
 * The number of generated elements is equal to (B-choose-N). For example, with two digits and a base of 100, this
 * iterator would generate 100*99/2 = 4950 elements.
 */
public class IncreasingCounter implements Iterator<int[]>, Iterable<int[]> {
    private final int mBase;
    private final int[] mDigits;
    private boolean mHasMore;

    public IncreasingCounter(int digitCount, int base) {
        mBase = base;
        mDigits = new int[digitCount];

        // First element.
        for (int i = 0; i < digitCount; i++) {
            mDigits[i] = i;
        }
        mHasMore = digitCount <= base;
    }

    @Override
    public boolean hasNext() {
        return mHasMore;
    }

    @Override
    public int[] next() {
        if (!mHasMore) {
            throw new NoSuchElementException();
        }

        int[] currentValue = Arrays.copyOf(mDigits, mDigits.length);

        // Pre-compute the next element and determine whether we have more.
        incrementDigits(mDigits.length, mBase);

        return currentValue;
    }

    @Override
    public Iterator<int[]> iterator() {
        // We have to create a new object, instead of just returning {@code this}, because an
        // Iterable can be used multiple times.
        return new IncreasingCounter(mDigits.length, mBase);
    }

    /**
     * Increments the first {@code length} digits of {@code mDigit} in base {@code base}. Sets mHasMore
     * to false if no more combinations are possible.
     */
    private void incrementDigits(int length, int base) {
        if (mHasMore) {
            if (length == 0) {
                mHasMore = false;
            } else {
                // Increment the digit we're responsible for.
                mDigits[length - 1]++;

                // See if we've exceeded the base.
                if (mDigits[length - 1] >= base) {
                    // Recurse to get next value of previous digits. Decrease base since those digits can't reach
                    // as far as we can.
                    incrementDigits(length - 1, base - 1);

                    // The recursion might have bottomed out, in which case we're done.
                    if (mHasMore) {
                        // Will always be valid.
                        mDigits[length - 1] = mDigits[length - 2] + 1;
                    }
                }
            }
        }
    }
}

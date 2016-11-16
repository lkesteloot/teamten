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

// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Vector;

import java.util.Comparator;

/**
 * Compares triangles for sorting. Uses their centroids.
 */
public class TriangleComparator implements Comparator<Triangle> {
    private final int mDimension;

    /**
     * Specifies which dimension (0, 1, or 2) to compare.
     */
    public TriangleComparator(int dimension) {
        mDimension = dimension;
    }

    @Override // Comparator
    public int compare(Triangle t1, Triangle t2) {
        // Compute the centroid of each.
        Vector c1 = t1.getCentroid();
        Vector c2 = t2.getCentroid();

        return Double.compare(c1.get(mDimension), c2.get(mDimension));
    }
}

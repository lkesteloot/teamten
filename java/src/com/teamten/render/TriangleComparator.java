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

// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Vector;

/**
 * Records the results of an intersection hit.
 */
public class Intersection {
    private Triangle mTriangle = null;
    private Vector mPoint = null;
    private double mMinT = Double.MAX_VALUE;
    private boolean mBackfacing = false;

    /**
     * Set all the fields at once.
     *
     * @param triangle the triangle that was hit, or null if none.
     * @param point the point in 3D where the ray hit the triangle.
     * @param minT the distance along the ray where it hit the triangle.
     * @param backfacing whether the triangle was facing away from the ray.
     */
    public void update(Triangle triangle, Vector point, double minT, boolean backfacing) {
        mTriangle = triangle;
        mPoint = point;
        mMinT = minT;
        mBackfacing = backfacing;
    }

    /**
     * Return the triangle that was intersected, or null if the ray hit nothing.
     */
    public Triangle getTriangle() {
        return mTriangle;
    }

    /**
     * Get the point in 3D space of the intersection.
     */
    public Vector getPoint() {
        return mPoint;
    }

    /**
     * The parametric distance along the ray to the intersection.
     */
    public double getMinT() {
        return mMinT;
    }

    /**
     * Whether our intersection hit the back of the triangle.
     */
    public boolean isBackfacing() {
        return mBackfacing;
    }

    /**
     * Utility method for getting the normal of the triangle, taking into
     * account whether it's backfacing.
     */
    public Vector getNormal() {
        Vector normal = mTriangle.getNormal();
        if (mBackfacing) {
            normal = normal.negate();
        }

        return normal;
    }
}

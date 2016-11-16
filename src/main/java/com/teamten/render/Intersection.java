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
     * Utility method for getting the normal of the triangle at the intersection
     * point (interpolating the vertex normals), taking into account whether it's
     * backfacing.
     */
    public Vector getNormal() {
        //   0: dot((point - v1), cache->h[1]) / dot((v0 - v1), cache->h[1]);
        //   1: dot((point - v2), cache->h[2]) / dot((v1 - v2), cache->h[2]);
        //   2: Different to make 1.
        // Compute barycentric coordinates.
        double b[] = new double[Triangle.NUM_VERTICES];
        for (int i = 0; i < Triangle.NUM_VERTICES; i++) {
            int next = (i + 1) % Triangle.NUM_VERTICES;

            Vector edgeNormal = mTriangle.getEdgeNormal(next);
            double numerator = mPoint.subtract(mTriangle.get(next).getPoint()).dot(edgeNormal);
            double denomenator = mTriangle.getEdge(i).negate().dot(edgeNormal);
            b[i] = numerator / denomenator;
        }

        // Normal is weighted average of vertex normals by barycentric coordinates.
        Vector normal = Vector.make(0, 0, 0);
        for (int i = 0; i < Triangle.NUM_VERTICES; i++) {
            normal = normal.add(mTriangle.get(i).getNormal().multiply(b[i]));
        }
        normal = normal.normalize();

        if (mBackfacing) {
            normal = normal.negate();
        }

        return normal;
    }
}

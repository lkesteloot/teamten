// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Represents an axis-aligned box that bounds some geometry, including a list
 * of that geometry and possible nested bounding boxes.
 */
public class BoundingBox {
    private static final boolean USE_MEDIAN = false;
    private static final int MIN_TRIANGLES = 5;
    private Vector mMin;
    private Vector mMax;
    private final List<Triangle> mTriangleList = new ArrayList<Triangle>();
    private final List<BoundingBox> mChildList = new ArrayList<BoundingBox>();

    /**
     * Create an infinite empty box.
     */
    public BoundingBox() {
        mMin = null;
        mMax = null;
    }

    public void addTriangle(Triangle triangle) {
        // Go through vertices of triangle.
        for (int i = 0; i < 3; i++) {
            Vector v = triangle.get(i);

            if (mMin == null) {
                mMin = v;
                mMax = v;
            } else {
                // Go through elements of vector.
                for (int j = 0; j < v.getSize(); j++) {
                    double vp = v.get(j);

                    if (vp < mMin.get(j)) {
                        mMin = mMin.with(j, vp);
                    }
                    if (vp > mMax.get(j)) {
                        mMax = mMax.with(j, vp);
                    }
                }
            }
        }

        mTriangleList.add(triangle);
    }

    /**
     * Returns the list of triangles in this bounding box.
     */
    public List<Triangle> getTriangleList() {
        return mTriangleList;
    }

    /**
     * Returns the list of child bounding boxes in this bounding box.
     */
    public List<BoundingBox> getChildList() {
        return mChildList;
    }

    /**
     * Return the diagonal size of the bounding box.
     */
    public double getSize() {
        if (mMax == null) {
            return 0;
        } else {
            return mMax.subtract(mMin).length();
        }
    }

    /**
     * Return the total number of children below this node, including this node.
     */
    public int getDeepChildCount() {
        int count = 1;

        for (BoundingBox child : mChildList) {
            count += child.getDeepChildCount();
        }

        return count;
    }

    /**
     * Returns whether the ray intersects the bounding box.
     */
    public boolean intersectsRay(Vector r0, Vector r, double maxT, boolean debug) {
        if (mMin == null) {
            return false;
        }

        if (debug) {
            System.out.printf("Testing intersection of %s,%s with max %g, %,d triangles%n",
                    r0, r, maxT, mTriangleList.size());
            System.out.printf("    Bounding box is %s to %s%n", mMin, mMax);
        }

        // Keep a segment along the ray between minT and maxT.
        double minT = 0;

        // Go through each dimension.
        for (int i = 0; i < mMin.getSize() && minT <= maxT; i++) {
            double v0 = r0.get(i);
            double v = r.get(i);
            double min = mMin.get(i);
            double max = mMax.get(i);

            if (v >= -1e-6 && v <= 1e-6) {
                // Parallel to slab. Don't update minT or maxT, but do check to
                // see if we're completely outside the slab in this dimension.
                if (v0 < min || v0 > max) {
                    if (debug) {
                        System.out.printf("Parallel to and outside of slab %d (%g,%g,%g)%n",
                                i, min, v0, max);
                    }
                    return false;
                }
            } else {
                // Intersect with both ends of the slab in this dimension.
                double t1 = (min - v0)/v;
                double t2 = (max - v0)/v;

                // Swap if we were facing the other way.
                if (t1 > t2) {
                    // Swap.
                    double t = t1;
                    t1 = t2;
                    t2 = t;
                }

                // Intersect with our current segment.
                minT = Math.max(minT, t1);
                maxT = Math.min(maxT, t2);
            }
        }
        if (debug) {
            System.out.printf("    Results: %g %g%n", minT, maxT);
        }

        // If the segment is empty, then the ray doesn't intersect with the box.
        return minT <= maxT;
    }

    /**
     * Build a tree below this node. Call this after having called addTriangle()
     * for all triangles.
     */
    public void createTree() {
        splitBoxInDimension(0, 0);
    }

    /**
     * Splits the contents of the box into two sub-boxes along dimension
     * "splitDimension".  If that's -1, auto-detect best dimension.
     */
    private void splitBoxInDimension(int splitDimension, int sameCount) {
        // Too few triangles to bother.
        if (mMin == null || mTriangleList.size() < MIN_TRIANGLES) {
            return;
        }

        /**
         * If we've gone three recursions in a row without decreasing the
         * number of triangles, then we're not making progress and must stop.
         */
        if (sameCount == 3) {
            if (mTriangleList.size() > 10) {
                System.out.printf("Warning: Can't break box with %d triangles.%n",
                        mTriangleList.size());
            }
            return;
        }

        Vector boxSize = mMax.subtract(mMin);

        if (splitDimension == -1) {
            // Find the largest axis.
            splitDimension = 0;
            for (int i = 1; i < boxSize.getSize(); i++) {
                if (boxSize.get(i) > boxSize.get(splitDimension)) {
                    splitDimension = i;
                }
            }
        }

        double splitPosition;
        if (USE_MEDIAN) {
            // Sort along the biggest dimension.
            Collections.sort(mTriangleList, new TriangleComparator(splitDimension));

            // Find the median centroid.
            Triangle medianTriangle = mTriangleList.get(mTriangleList.size()/2);
            Vector mediaTriangeCentroid = medianTriangle.getCentroid();

            // Median:
            splitPosition = mediaTriangeCentroid.get(splitDimension);
        } else {
            // Average:
            splitPosition = mMin.get(splitDimension) + boxSize.get(splitDimension)/2;
        }

        // Create two children bounding boxes, split at the split position.
        BoundingBox lessBoundingBox = new BoundingBox();
        BoundingBox moreBoundingBox = new BoundingBox();

        // Put each triangle into the appropriate boxes.
        for (Triangle triangle : mTriangleList) {
            boolean anyLess = false;
            boolean anyMore = false;

            for (int i = 0; i < Triangle.NUM_VERTICES; i++) {
                double p = triangle.get(i).get(splitDimension);

                if (p <= splitPosition) {
                    anyLess = true;
                    break;
                }
                if (p >= splitPosition) {
                    anyMore = true;
                }
            }

            // Only go into one child.
            if (anyLess) {
                lessBoundingBox.addTriangle(triangle);
            } else if (anyMore) {
                moreBoundingBox.addTriangle(triangle);
            }
        }

        // If either side got all triangles, then we're making no progress and
        // must stop.
        int lessSize = lessBoundingBox.getTriangleList().size();
        int moreSize = moreBoundingBox.getTriangleList().size();
        int totalSize = mTriangleList.size();
        if (false && (lessSize == totalSize || moreSize == totalSize)) {
            // We're done.
            if (mTriangleList.size() > 20) {
                System.out.printf("Warning: Can't break box with %d triangles.%n",
                        mTriangleList.size());
            }
            /*
            System.out.printf("Box at %s to %s couldn't be broken (%d triangles)%n",
                    mMin, mMax, mTriangleList.size());
            System.out.printf("    Split was at %g in dimension %d%n",
                    splitPosition, splitDimension);
            System.out.printf("    Box size is %s%n", boxSize);
            // System.out.printf("    Median triangle centroid is %s%n", mediaTriangeCentroid);
            for (Triangle triangle : mTriangleList) {
                System.out.printf("    %s%n", triangle);
            }
            */
        } else {
            // Both got smaller. Keep going.
            mChildList.add(lessBoundingBox);
            mChildList.add(moreBoundingBox);

            // XXX won't work with -1 passed in.
            int nextSplitDimension = (splitDimension + 1) % 3;
            lessBoundingBox.splitBoxInDimension(nextSplitDimension,
                    lessSize == totalSize ? sameCount + 1 : 0);
            moreBoundingBox.splitBoxInDimension(nextSplitDimension,
                    moreSize == totalSize ? sameCount + 1 : 0);
        }
    }

    /**
     * Break up triangles that are larger than maxRatio of the bounding box itself.
     */
    public void breakUpLargeTriangles(double maxRatio) {
        double totalSize = getSize();
        double maxSize = totalSize * maxRatio;

        for (int i = 0; i < mTriangleList.size(); ) {
            Triangle triangle = mTriangleList.get(i);

            BoundingBox triangleBox = new BoundingBox();
            triangleBox.addTriangle(triangle);

            // See if we're too big.
            if (triangleBox.getSize() > maxSize) {
                List<Triangle> subtriangles = triangle.tesselate();

                // Replace the current triangle.
                mTriangleList.set(i, subtriangles.get(0));

                // Append the rest.
                mTriangleList.addAll(subtriangles.subList(1, subtriangles.size()));

                // Don't increment the counter, we want to test the first new
                // subtriangle. In principle this could continue forever.
            } else {
                i++;
            }
        }
    }
}

// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Matrix;
import com.teamten.math.Vector;

import java.util.Arrays;
import java.util.List;

/**
 * Represents an immutable triangle in 3D.
 *
 * TODO: We should keep other information with each vertex, including surface
 * normal and color.  Not sure how to do that and keep it immutable. Maybe with
 * a builder? With so few parameters, probably just make a constructor and
 * allow nulls for unused parameters.
 */
public class Triangle {
    public static final int NUM_VERTICES = 3;
    private final Vector[] mVertices = new Vector[NUM_VERTICES];
    private Vector mNormal = null;
    private Vector mCentroid = null;

    public Triangle(Vector ... vertices) {
        if (vertices.length != NUM_VERTICES) {
            throw new IllegalArgumentException("Triangles must have three vertices");
        }

        for (int i = 0; i < NUM_VERTICES; i++) {
            mVertices[i] = vertices[i];
        }
    }

    /**
     * Return a new triangle that's been translated by "offset".
     */
    public Triangle withOffset(Vector offset) {
        Vector[] newVertices = new Vector[NUM_VERTICES];

        for (int i = 0; i < NUM_VERTICES; i++) {
            newVertices[i] = mVertices[i].add(offset);
        }

        return new Triangle(newVertices);
    }

    /**
     * Return vertex at index (0, 1, or 2), with no bounds checking.
     */
    public Vector get(int index) {
        return mVertices[index];
    }

    /**
     * Return the geometric normal of the triangle. The normal will point toward you if
     * the vertices are in clockwise order.
     */
    public Vector getNormal() {
        // Cache the normal.
        if (mNormal == null) {
            // Right-hand rule.
            mNormal = mVertices[1].subtract(mVertices[2]).
                cross(mVertices[0].subtract(mVertices[1])).
                normalize();
        }

        return mNormal;
    }

    /**
     * Returns this triangle transformed by the 4x4 matrix.
     */
    public Triangle transform(Matrix matrix) {
        Vector[] newVertices = new Vector[NUM_VERTICES];

        for (int i = 0; i < NUM_VERTICES; i++) {
            newVertices[i] = matrix.transform(mVertices[i]);
        }

        return new Triangle(newVertices);
    }

    /**
     * Return the centroid of the triangle, which is just the average of the three
     * vertices.
     */
    public Vector getCentroid() {
        // Cache the centroid.
        if (mCentroid == null) {
            mCentroid = mVertices[0].add(mVertices[1]).add(mVertices[2]).multiply(1/3.0);
        }

        return mCentroid;
    }

    /**
     * Return a list of four triangles that make this triangle. Splits each of
     * the three edges, connects the three split points to make the center
     * triangle, and also returns the our three triangles. Each returned
     * triangle is guaranteed to be smaller in all dimensions than the
     * original.
     */
    public List<Triangle> tesselate() {
        // Calculate the three midpoints.
        Vector vertex01 = mVertices[0].add(mVertices[1]).multiply(0.5);
        Vector vertex12 = mVertices[1].add(mVertices[2]).multiply(0.5);
        Vector vertex20 = mVertices[2].add(mVertices[0]).multiply(0.5);

        return Arrays.asList(
                new Triangle(mVertices[0], vertex01, vertex20),
                new Triangle(vertex01, mVertices[1], vertex12),
                new Triangle(vertex20, vertex12, mVertices[2]),
                new Triangle(vertex12, vertex20, vertex01));
    }

    @Override // Object
    public String toString() {
        return mVertices[0] + " " + mVertices[1] + " " + mVertices[2];
    }
}

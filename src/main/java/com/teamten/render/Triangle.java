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

import com.teamten.math.Matrix;
import com.teamten.math.Vector;

import java.util.Arrays;
import java.util.List;

/**
 * Represents an immutable triangle in 3D.
 */
public class Triangle {
    public static final int NUM_VERTICES = 3;
    private final Vertex[] mVertices = new Vertex[NUM_VERTICES];
    private final Vector mNormal;
    private final Vector mCentroid;
    // Edge between index and index - 1.
    private final Vector[] mEdges = new Vector[NUM_VERTICES];
    private final Vector[] mEdgeNormals = new Vector[NUM_VERTICES];

    public Triangle(Vertex ... vertices) throws DegenerateTriangleException {
        if (vertices.length != NUM_VERTICES) {
            throw new IllegalArgumentException("Triangles must have three vertices");
        }

        for (int i = 0; i < NUM_VERTICES; i++) {
            mVertices[i] = vertices[i];
        }

        // Compute edge vectors.
        for (int i = 0; i < NUM_VERTICES; i++) {
            int next = (i + 1) % NUM_VERTICES;
            mEdges[i] = mVertices[next].getPoint().subtract(mVertices[i].getPoint());
        }

        // Compute geometric normal and centroid.
        try {
            mNormal = mEdges[1].cross(mEdges[0]).normalize();
        } catch (IllegalArgumentException e) {
            throw new DegenerateTriangleException();
        }
        mCentroid = mVertices[0].getPoint().add(
                mVertices[1].getPoint()).add(
                mVertices[2].getPoint()).multiply(1/3.0);

        // Compute edge normals in the plane.
        for (int i = 0; i < NUM_VERTICES; i++) {
            mEdgeNormals[i] = mNormal.cross(mEdges[i]);
        }
    }

    /**
     * Return vertex at index (0, 1, or 2), with no bounds checking.
     */
    public Vertex get(int index) {
        return mVertices[index];
    }

    /**
     * Return the geometric normal of the triangle. The normal will point toward you if
     * the vertices are in clockwise order.
     */
    public Vector getNormal() {
        return mNormal;
    }

    /**
     * Return the centroid of the triangle, which is just the average of the three
     * vertices.
     */
    public Vector getCentroid() {
        return mCentroid;
    }

    /**
     * Return the vector from vertex index to index + 1. This is not normalized.
     */
    public Vector getEdge(int index) {
        return mEdges[index];
    }

    /**
     * Return a normal to edge from vertex index to index + 1. This is not
     * normalized.
     */
    public Vector getEdgeNormal(int index) {
        return mEdgeNormals[index];
    }

    /**
     * Return a list of four triangles that make this triangle. Splits each of
     * the three edges, connects the three split points to make the center
     * triangle, and also returns the our three triangles. Each returned
     * triangle is guaranteed to be smaller in all dimensions than the
     * original.
     */
    public List<Triangle> tesselate() {
        /*
        // Calculate the three midpoints.
        Vector vertex01 = mVertices[0].add(mVertices[1]).multiply(0.5);
        Vector vertex12 = mVertices[1].add(mVertices[2]).multiply(0.5);
        Vector vertex20 = mVertices[2].add(mVertices[0]).multiply(0.5);

        return Arrays.asList(
                new Triangle(mVertices[0], vertex01, vertex20),
                new Triangle(vertex01, mVertices[1], vertex12),
                new Triangle(vertex20, vertex12, mVertices[2]),
                new Triangle(vertex12, vertex20, vertex01));
        */
        throw new IllegalStateException("tesselate() not implemented");
    }

    @Override // Object
    public String toString() {
        return mVertices[0] + " " + mVertices[1] + " " + mVertices[2];
    }
}

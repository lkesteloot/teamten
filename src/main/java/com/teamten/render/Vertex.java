// Copyright 2013 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Vector;

/**
 * Immutable structure to store all information about a triangle vertex.
 */
public class Vertex {
    private final Vector mPoint;
    private final Vector mNormal;

    public Vertex(Vector point, Vector normal) {
        mPoint = point;
        mNormal = normal;
    }

    public Vector getPoint() {
        return mPoint;
    }

    public Vector getNormal() {
        return mNormal;
    }
}

// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Matrix;
import com.teamten.math.Vector;

/**
 * Represents a light source and the algorithm to light a point.
 */
public interface Light {
    /**
     * Return a unit vector from the intersection point to the light. The
     * intersection must not be empty and must be in camera space.
     */
    Vector getLightVector(Intersection intersection);

    /**
     * Return the color of the light at this intersection point. The
     * intersection must not be empty and must be in camera space.
     */
    Color getLightColor(Intersection intersection);

    /**
     * Sets the camera transform for this pass.
     */
    void setCamera(Matrix camera);
}

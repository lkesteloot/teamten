// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Vector;

/**
 * Interface for tracing rays and shading points.
 */
public interface RayTracer {
    /**
     * Return the intersection information for a ray starting at r0 and heading in the
     * direction of vector r.  The returned object is never null, but its
     * triangle will be null if the ray didn't hit anything.
     */
    Intersection intersect(Vector r0, Vector r, boolean debug);

    /**
     * Shade the point at the intersection (whose triangle is not null), given that
     * the eye is coming in the direction of "eye".
     */
    Color shade(Vector eye, Intersection intersection, boolean debug);
}


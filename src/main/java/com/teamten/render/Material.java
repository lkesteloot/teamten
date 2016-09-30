// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Vector;

/**
 * Can shade a material given a list of lights and an intersection point.
 */
public interface Material {
    /**
     * Return the shaded point for this intersection. The intersection must not
     * be empty and must be in camera space.
     *
     * @param eye vector from eye, not normalized.
     * @return color of shaded point, never null.
     */
    Color shade(RayTracer rayTracer, Vector eye, Intersection intersection,
            Light[] lightList, boolean debug);
}

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


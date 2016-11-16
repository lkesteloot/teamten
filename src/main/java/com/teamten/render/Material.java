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

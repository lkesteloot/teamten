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

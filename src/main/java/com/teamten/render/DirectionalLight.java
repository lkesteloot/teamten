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
 * Represents an infinitely far away directional light.
 */
public class DirectionalLight implements Light {
    private final Vector mWorldSpace;
    private final Color mColor;
    private Vector mCameraSpace;

    /**
     * Specifies the direction the light is traveling. Does not have to be normalized.
     */
    DirectionalLight(Vector directionFromLight, Color color) {
        mWorldSpace = directionFromLight.negate().normalize();
        mColor = color;

        mCameraSpace = mWorldSpace;
    }

    @Override // Light
    public Vector getLightVector(Intersection intersection) {
        return mCameraSpace;
    }

    @Override // Light
    public Color getLightColor(Intersection intersection) {
        return mColor;
    }

    @Override // Light
    public void setCamera(Matrix camera) {
        mCameraSpace = camera.transformVector(mWorldSpace);
    }
}


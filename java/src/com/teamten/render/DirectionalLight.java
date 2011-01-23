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


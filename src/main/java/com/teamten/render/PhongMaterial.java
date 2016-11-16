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
 * Implementation of the Phong shading model.
 */
public class PhongMaterial implements Material {
    public static final PhongMaterial DEFAULT = new PhongMaterial(
             new Color(1, 0.0, 0.0, 0.0),
             new Color(1, 0.9, 0.9, 0.9),
             new Color(1, 0.5, 0.5, 0.5), 100);
    private final Color mAmbientColor;
    private final Color mDiffuseColor;
    private final Color mSpecularColor;
    private final double mSpecularPower;

    public PhongMaterial(Color ambientColor, Color diffuseColor, Color specularColor,
            double specularPower) {

        mAmbientColor = ambientColor;
        mDiffuseColor = diffuseColor;
        mSpecularColor = specularColor;
        mSpecularPower = specularPower;
    }

    @Override // Material
    public Color shade(RayTracer rayTracer, Vector eye, Intersection intersection,
            Light[] lightList, boolean debug) {

        Color color = mAmbientColor;
        eye = eye.negate().normalize();

        Vector normal = intersection.getNormal();

        // Add up contribution of all lights.
        for (Light light : lightList) {
            Vector lightVector = light.getLightVector(intersection);
            Color lightColor = light.getLightColor(intersection);

            double diffuse = normal.dot(lightVector);
            if (diffuse < 0) {
                diffuse = 0;
            }
            color = color.add(lightColor.multiply(mDiffuseColor).multiply(diffuse));

            Vector reflection = eye.subtract(eye.subtract(
                        normal.multiply(eye.dot(normal))).multiply(2));
            double specular = reflection.dot(lightVector);
            if (specular < 0) {
                specular = 0;
            }
            specular = Math.pow(specular, mSpecularPower);
            color = color.add(lightColor.multiply(mSpecularColor).multiply(specular));
        }

        return color;
    }
}

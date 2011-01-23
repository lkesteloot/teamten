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
             new Color(1, 0.1, 0.1, 0.1), 10);
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

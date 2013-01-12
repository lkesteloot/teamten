// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.math.Vector;

/**
 * Implements the ambient occlusion material.
 */
public class AmbientOcclusionMaterial implements Material {
    private final Color mSurfaceColor;
    private final int mGridWidth;
    private final int mGridHeight;
    private final int mRayCount;

    /**
     * Samples are sent out in a grid across the hemisphere. The grid width and
     * height specify the number of points on this grid.
     */
    public AmbientOcclusionMaterial(Color surfaceColor, int gridWidth, int gridHeight) {
        mSurfaceColor = surfaceColor;
        mGridWidth = gridWidth;
        mGridHeight = gridHeight;
        mRayCount = mGridWidth * mGridHeight;
    }

    @Override // Material
    public Color shade(RayTracer rayTracer, Vector eye, Intersection intersection,
            Light[] lightList, boolean debug) {

        // Lights are ignored.

        Vector point = intersection.getPoint();
        Vector normal = intersection.getNormal();

        // Move past the original intersection point in case we have backface culling
        // disabled. This epsilon is a bit disturbing.
        point = point.add(normal.multiply(0.0001));

        // Find one triangle edge.
        Vector vertex0 = intersection.getTriangle().get(0).getPoint();
        Vector vertex1 = intersection.getTriangle().get(1).getPoint();
        Vector edge = vertex1.subtract(vertex0);

        // Calculate two vectors for the surface plane. Doesn't matter which
        // way they face, so pick one edge and cross to get the third.
        Vector x = edge.normalize();
        Vector z = x.cross(normal);

        double brightness = 0.0;
        double total = 0.0;

        for (int i = 0; i < mRayCount; i++) {
            double dx;
            double dy;
            double dz;

            // Generate rays within the unit hemisphere.
            if (false) {
                // Uniform random sampling.
                do {
                    dx = Math.random()*2 - 1;
                    dy = Math.random();
                    dz = Math.random()*2 - 1;
                } while (dx*dx + dy*dy + dz*dz > 1);
            } else {
                // Uniform grid sampling. Pick point on cylinder.
                /*
                double latitude = Math.random()*Math.PI/2;
                double longitude = Math.random()*Math.PI*2;
                */
                double latitude = (i % mGridWidth + Math.random()) / mGridWidth * Math.PI/2;
                double longitude = (i / mGridWidth + Math.random()) / mGridHeight * Math.PI*2;

                double dist = Math.cos(latitude);
                dx = Math.sin(longitude)*dist;
                dy = Math.sin(latitude);
                dz = Math.cos(longitude)*dist;
            }

            Vector v = normal.multiply(dy).add(x.multiply(dx)).add(z.multiply(dz)).normalize();

            // Cosine law, like diffuse.
            double contribution = v.dot(normal);
            total += contribution;

            // We only care about rays that hit the sky (Y >= 0).
            if (v.get(1) >= 0) {
                Intersection newIntersection = rayTracer.intersect(point, v, debug);
                if (newIntersection.getTriangle() == null) {
                    brightness += contribution;
                }
            }
        }

        // Normalize.
        if (total != 0) {
            brightness /= total;
        }

        return mSurfaceColor.multiply(brightness);
    }
}

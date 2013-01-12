// Copyright 2011 Lawrence Kesteloot

package com.teamten.render;

import com.teamten.image.ImageUtils;
import com.teamten.math.Matrix;
import com.teamten.math.Vector;
import com.teamten.util.Dates;

import java.awt.image.BufferedImage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A renderer from geometry to image files.
 */
public class Renderer {
    // Set this to zero because it doesn't appear to help.
    private static final double TESSELATE_RATIO = 0.0;
    private static final boolean PRINT_RENDER_STATS = false;
    private final List<Triangle> mTriangleList = new ArrayList<Triangle>();
    private final Light[] mLightList = new Light[] {
        new DirectionalLight(Vector.make(0, 1, 0), new Color(1, 0.5, 0.5, 0.5)),
        new DirectionalLight(Vector.make(-1, -1, -1), new Color(1, 0.5, 0.5, 0.5))
    };
    private Material mMaterial = PhongMaterial.DEFAULT;
    private Matrix mCamera = Matrix.makeUnit(4);
    private Matrix mCameraInverse = mCamera.getInverse();
    private AtomicInteger mTriangleIntersectionCount = new AtomicInteger();
    private AtomicInteger mRayCount = new AtomicInteger();
    private double mHorizontalFov = Math.PI/6;
    private double mVerticalFov = Math.PI/6;
    private final int mSuperSample;
    private boolean mCullBackfacingTriangles = true;

    // Precomputed data:
    private BoundingBox mBoundingBox = null;

    /**
     * Creates a renderer with the specified parameters.
     *
     * @param superSample the square root of the number of anti-aliased samples.
     */
    public Renderer(int superSample) {
        mSuperSample = superSample;
    }

    /**
     * Whether to hide triangles that face away from the camera. Defaults to
     * true.
     */
    public void setCullBackfacingTriangles(boolean cullBackfacingTriangles) {
        mCullBackfacingTriangles = cullBackfacingTriangles;
    }

    /**
     * Position the camera at "eye" looking at "target" with "up"
     * either specified or pointing to the Y axis if null.
     */
    public void lookAt(Vector eye, Vector target, Vector up) {
        Vector y = up == null ? Vector.Y : up.normalize();
        Vector z = eye.subtract(target).normalize();
        Vector x = y.cross(z).normalize();
        y = z.cross(x).normalize();

        mCamera = Matrix.makeWithBasis(x, y, z).multiply(Matrix.makeTranslation(eye.negate()));
        mCameraInverse = mCamera.getInverse();
    }

    /**
     * Sets the full horizontal and vertical field of view in radians.
     */
    public void setFov(double horizontalFov, double verticalFov) {
        mHorizontalFov = horizontalFov;
        mVerticalFov = verticalFov;
    }

    /**
     * Sets the material for all geometry.
     */
    public void setMaterial(Material material) {
        mMaterial = material;
    }

    /**
     * Add a triangle to the geometry being rendered.
     */
    public void addTriangle(Triangle triangle) {
        mTriangleList.add(triangle);
    }

    /**
     * Precompute various geometry things, like bounding boxes.
     */
    public void prepareGeometry() {
        // Add everything to the top bounding box.
        mBoundingBox = new BoundingBox();
        for (Triangle triangle : mTriangleList) {
            mBoundingBox.addTriangle(triangle);
        }

        // Break up large triangles because they make it hard to create
        // bounding box trees. This doesn't appear to help -- the new triangles
        // have the same aspect ratio and continue to cause problems with
        // the bounding boxes.
        if (TESSELATE_RATIO > 0) {
            mBoundingBox.breakUpLargeTriangles(TESSELATE_RATIO);
        }

        // Create the bounding box hierarchy.
        long beforeTime = System.currentTimeMillis();
        mBoundingBox.createTree();
        long afterTime = System.currentTimeMillis();
        long createTreeTime = afterTime - beforeTime;

        System.out.printf("Number of initial triangles: %,d%n", mTriangleList.size());
        System.out.printf("Number of final triangles:   %,d%n",
                mBoundingBox.getTriangleList().size());
        System.out.printf("Number of bounding boxes:    %,d%n", mBoundingBox.getDeepChildCount());
        System.out.printf("Create tree time:            %,d ms%n", createTreeTime);
    }

    /**
     * Generate an image of size width and height. U and v are the upper-left corner
     * of the image on a 0 to 1 scale. Du and dv are the width and height on that same
     * scale.
     */
    public BufferedImage render(final int width, final int height,
            final float u, final float v,
            final float du, final float dv) {
        final BufferedImage image = ImageUtils.makeTransparent(width, height);
        final Vector eye = mCameraInverse.transform(Vector.make(0, 0, 0));

        // Create a RayTracer object that shaders can use to trace more rays in
        // the scene.
        final RayTracer rayTracer = new RayTracer() {
            @Override // RayTracer
            public Intersection intersect(Vector r0, Vector r, boolean debug) {
                return Renderer.this.intersect(r0, r, mBoundingBox, debug);
            }

            @Override // RayTracer
            public Color shade(Vector eye, Intersection intersection, boolean debug) {
                return Renderer.this.shade(this, eye, intersection, debug);
            }
        };

        int availableProcessors = Runtime.getRuntime().availableProcessors();
        /// availableProcessors = 1;
        if (PRINT_RENDER_STATS) {
            System.out.println("Parallelizing across " + availableProcessors + " processors");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(availableProcessors);

        mTriangleIntersectionCount.set(0);
        mRayCount.set(0);
        final AtomicInteger completedRowsCount = new AtomicInteger();

        final long beforeRenderTime = System.currentTimeMillis();
        final AtomicLong previousNotice = new AtomicLong();
        previousNotice.set(beforeRenderTime);

        // Calculate the sides of the full frame based on the FOV and assume a Z component
        // of 1.
        final double right = Math.tan(mHorizontalFov/2);
        final double left = -right;
        final double top = Math.tan(mVerticalFov/2);
        final double bottom = -top;

        for (int y = 0; y < height; y++) {
            final int finalY = y;
            final double dy = bottom + (v + dv*y)*(top - bottom);

            // Submit entire rows to the executor.
            executorService.submit(new Runnable() {
                public void run() {
                    for (int x = 0; x < width; x++) {
                        double dx = left + (u + du*x)*(right - left);

                        Color pixelColor = Color.BLACK;

                        for (int sy = 0; sy < mSuperSample; sy++) {
                            for (int sx = 0; sx < mSuperSample; sx++) {
                                Vector ray = Vector.make(
                                    dx + (double) sx/mSuperSample,
                                    dy + (double) sy/mSuperSample,
                                    -1);

                                // Transform by camera.
                                ray = mCameraInverse.transform(ray).subtract(eye);

                                boolean debug = false;
                                /// debug = x == width/2 && finalY == height/2; // Center
                                /// debug = x == width - 1 && finalY == 0; // Upper-right
                                /// debug = x == width*2/3 && finalY == height*2/3; // Off-center
                                /// debug = x == width/2 && finalY == height*9/10; // Bottom center

                                // Intersect with geometry.
                                Intersection intersection = intersect(eye, ray, mBoundingBox, debug);

                                // Determine color of pixel.
                                Color color;
                                if (intersection.getTriangle() == null) {
                                    // Background.
                                    color = new Color(1, 0.5, 0.75, 1);
                                    color = new Color(1, 0, 0, 0);
                                } else {
                                    color = shade(rayTracer, ray, intersection, debug);
                                }

                                pixelColor = pixelColor.add(color);
                            }
                        }

                        image.setRGB(x, finalY, pixelColor.multiply(1.0/mSuperSample/mSuperSample).
                                clamp().toArgb());
                    }

                    synchronized (completedRowsCount) {
                        completedRowsCount.incrementAndGet();

                        long now = System.currentTimeMillis();
                        if (now - previousNotice.get() >= 1000) {
                            previousNotice.set(now);
                            int completedRows = completedRowsCount.get();
                            long estimatedTimeLeft = Dates.estimateTimeLeft(beforeRenderTime,
                                    now, completedRows, height);
                            if (PRINT_RENDER_STATS) {
                                System.out.printf("Completed %d rows of %d (%s left)%n",
                                        completedRows, height,
                                        Dates.durationToString(estimatedTimeLeft));
                            }
                        }
                    }

                }
            });
        }

        executorService.shutdown();

        try {
            executorService.awaitTermination(100, TimeUnit.DAYS);
        } catch (InterruptedException e) {
            System.out.println("Rendering interrupted...");
            Thread.currentThread().interrupt();
        }

        long afterTime = System.currentTimeMillis();
        long renderTime = afterTime - beforeRenderTime;

        if (PRINT_RENDER_STATS) {
            System.out.printf("Triangle intersections:      %,d (%.1f per pixel, %.1f per ray)%n",
                    mTriangleIntersectionCount.get(),
                    (double) mTriangleIntersectionCount.get() / width / height,
                    (double) mTriangleIntersectionCount.get() / mRayCount.get());
            System.out.printf("Render time:                 %,d ms%n", renderTime);
        }

        return image;
    }

    /**
     * Return the result of intersecting a ray starting at r0 toward r.
     */
    private Intersection intersect(Vector r0, Vector r, BoundingBox boundingBox, boolean debug) {
        mRayCount.incrementAndGet();

        Intersection intersection = new Intersection();

        if (false) {
            // Brute force.
            for (Triangle triangle : mTriangleList) {
                intersectTriangle(r0, r, triangle, intersection, debug);
            }
        } else {
            // Use bounding boxes.
            intersectBoundingBox(r0, r, boundingBox, intersection, debug);
        }

        return intersection;
    }

    /**
     * Intersect the ray (r0,r) with the triangle, updating "intersection"
     * if necessary.
     */
    private void intersectTriangle(Vector r0, Vector r, Triangle triangle,
            Intersection intersection, boolean debug) {

        // The plane is perpendicular to "normal" and goes through "v0".
        Vector normal = triangle.getNormal();
        boolean backfacing = normal.dot(r) > 0;
        if (backfacing && mCullBackfacingTriangles) {
            // Back-facing.
            return;
        }

        mTriangleIntersectionCount.incrementAndGet();

        Vector v0 = triangle.get(0).getPoint();
        Vector v1 = triangle.get(1).getPoint();
        Vector v2 = triangle.get(2).getPoint();

        // Intersect with plane.
        double denom = normal.dot(r);
        if (denom != 0) {
            double t = -normal.dot(r0.subtract(v0)) / denom;

            // See if the plane is closer than what we have so far, but not
            // behind us.
            if (t > 0 && t < intersection.getMinT()) {
                // See if we're inside the triangle.
                Vector p = r0.add(r.multiply(t));

                // We must be on the same side of all lines.
                Vector c1 = v0.subtract(v1).cross(p.subtract(v0));
                Vector c2 = v1.subtract(v2).cross(p.subtract(v1));
                Vector c3 = v2.subtract(v0).cross(p.subtract(v2));

                boolean dot1 = c1.dot(normal) >= 0;
                boolean dot2 = c2.dot(normal) >= 0;
                boolean dot3 = c3.dot(normal) >= 0;

                if (dot1 == dot2 && dot2 == dot3) {
                    intersection.update(triangle, p, t, backfacing);
                }
            }
        }
    }

    /**
     * Intersect the ray (r0,r) with the bounding box (and its contents), updating
     * "intersection" if necessary.
     */
    private void intersectBoundingBox(Vector r0, Vector r, BoundingBox boundingBox,
            Intersection intersection, boolean debug) {

        if (boundingBox.intersectsRay(r0, r, intersection.getMinT(), debug)) {
            List<BoundingBox> childList = boundingBox.getChildList();

            if (childList.isEmpty()) {
                // Leaf node, intersect all triangles.
                if (debug) {
                    System.out.printf("Testing intersection with %d triangles%n",
                            boundingBox.getTriangleList().size());
                }
                for (Triangle triangle : boundingBox.getTriangleList()) {
                    intersectTriangle(r0, r, triangle, intersection, debug);
                }
            } else {
                // Recurse.
                for (BoundingBox child : childList) {
                    intersectBoundingBox(r0, r, child, intersection, debug);
                }
            }
        }
    }

    /**
     * Share an intersected point. Just delegates to the material.
     */
    private Color shade(RayTracer rayTracer, Vector eye, Intersection intersection,
            boolean debug) {

        return mMaterial.shade(rayTracer, eye, intersection, mLightList, debug);
    }
}

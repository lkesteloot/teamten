// Copyright 2012 Lawrence Kesteloot

package com.teamten.render;

import com.google.common.io.LittleEndianDataInputStream;
import com.google.common.io.LittleEndianDataOutputStream;

import com.teamten.image.ImageUtils;
import com.teamten.math.Vector;
import com.teamten.render.Renderer;
import com.teamten.render.Triangle;

import java.awt.image.BufferedImage;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.EOFException;
import java.io.IOException;

import java.net.Socket;

/**
 * Serves a particular connection from an URT controller.
 */
public class UrtConnection extends Thread {
    private static final boolean DEBUG_PRINT = false;
    private static final int REQUEST_TYPE_TRACE_TILE = 1;
    private static final int REQUEST_TYPE_ADD_TRIANGLES = 2;
    private static final int REQUEST_TYPE_SET_CAMERA = 3;
    private final Socket mSocket;
    private final Renderer mRenderer;
    private BufferedImage mRenderedImage = null;

    public UrtConnection(Socket socket) {
        mSocket = socket;
        mRenderer = new Renderer(1);
        mRenderer.lookAt(Vector.make(2, 2, 2), Vector.make(0, 0, 0));
    }

    @Override
    public void run() {
        try {
            LittleEndianDataInputStream is = new LittleEndianDataInputStream(
                    new BufferedInputStream(mSocket.getInputStream()));
            LittleEndianDataOutputStream os = new LittleEndianDataOutputStream(
                    new BufferedOutputStream(mSocket.getOutputStream()));

            // Accept requests indefinitely.
            while (true) {
                int length = is.readInt();
                if (DEBUG_PRINT) {
                    System.out.println("Length is " + length);
                }

                int requestType = is.readInt();
                if (DEBUG_PRINT) {
                    System.out.println("Request type is " + requestType);
                }

                switch (requestType) {
                    case REQUEST_TYPE_TRACE_TILE:
                        traceTile(is, os);
                        break;

                    case REQUEST_TYPE_ADD_TRIANGLES:
                        addTriangles(is, os);
                        break;

                    case REQUEST_TYPE_SET_CAMERA:
                        setCamera(is, os);
                        break;

                    default:
                        // Already read the request type.
                        int skipping = length - 4;
                        System.err.println("Request type " + requestType +
                                " unknown, skipping " + skipping + " bytes");
                        is.skipBytes(skipping);
                        break;
                }

                os.flush();
            }
        } catch (EOFException e) {
            System.err.println("Client closed connection");
        } catch (IOException e) {
            System.err.println("Got exception reading from stream: " + e);
        }
    }

    private void traceTile(DataInput is, DataOutput os) throws IOException {
        float u = is.readFloat();
        float v = is.readFloat();
        float du = is.readFloat();
        float dv = is.readFloat();
        int w = is.readInt();
        int h = is.readInt();
        if (DEBUG_PRINT) {
            System.out.printf("traceTile(%g,%g,%g,%g,%d,%d)%n", u, v, du, dv, w, h);
        }

        int elementCount = w*h*3;
        os.writeInt(elementCount*4); // Length

        if (false) {
            float image[] = new float[elementCount];
            int index = 0;
            for (int j = 0; j < h; j++) {
                for (int i = 0; i < w; i++) {
                    float ru = u + du*i;
                    float rv = v + dv*j;
                    image[index++] = ru;
                    image[index++] = rv;
                    image[index++] = 1.0f;
                }
            }

            for (float f : image) {
                os.writeFloat(f);
            }
        } else {
            if (mRenderedImage == null) {
                mRenderedImage = mRenderer.render(512, 512);
                ImageUtils.save(mRenderedImage, "out.png");
            }
            int startX = (int) (u*512);
            int startY = (int) (v*512);
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    int xx = startX + x;
                    int yy = 511 - (startY + y);
                    int rgb = mRenderedImage.getRGB(xx, yy);

                    // This seems to be backward.
                    int red = rgb & 0xFF;
                    int green = (rgb >> 8) & 0xFF;
                    int blue = (rgb >> 16) & 0xFF;

                    float rf = red/255.0f;
                    float gf = green/255.0f;
                    float bf = blue/255.0f;

                    os.writeFloat(rf);
                    os.writeFloat(gf);
                    os.writeFloat(bf);
                }
            }
        }
    }

    private void addTriangles(DataInput is, DataOutput os) throws IOException {
        // Clear old image.
        mRenderedImage = null;

        int maxTriangles = is.readInt();
        int triangleCount = is.readInt();

        for (int i = 0; i < maxTriangles; i++) {
            float v1x = is.readFloat();
            float v1y = is.readFloat();
            float v1z = is.readFloat();
            float v2x = is.readFloat();
            float v2y = is.readFloat();
            float v2z = is.readFloat();
            float v3x = is.readFloat();
            float v3y = is.readFloat();
            float v3z = is.readFloat();

            if (i < triangleCount) {
                Triangle triangle = new Triangle(
                    Vector.make(v3x, v3y, v3z),
                    Vector.make(v2x, v2y, v2z),
                    Vector.make(v1x, v1y, v1z));
                mRenderer.addTriangle(triangle);
            }
        }
    }

    private void setCamera(DataInput is, DataOutput os) throws IOException {
        float x, y, z;

        // Eye.
        x = is.readFloat();
        y = is.readFloat();
        z = is.readFloat();
        Vector eye = Vector.make(x, y, z);

        // Target.
        x = is.readFloat();
        y = is.readFloat();
        z = is.readFloat();
        Vector target = Vector.make(x, y, z);

        System.out.printf("Setting camera to %s to %s%n", eye, target);

        x = is.readFloat();
        y = is.readFloat();
        z = is.readFloat();

        is.readFloat();
        is.readFloat();
        is.readInt();

        mRenderer.lookAt(eye, target);

        // Invalidate cache.
        mRenderedImage = null;
    }
}

// Copyright 2011 Lawrence Kesteloot

package com.teamten.image;

/**
 * Specifies the algorithm used to blend two images together.
 */
public enum BlendingMode {
    /**
     * Normal alpha blending.
     */
    NORMAL,

    /**
     * The result is the inverse of the product of the inverses. It always lightens
     * the image.
     */
    SCREEN
}

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

package com.teamten.typeset;

import com.teamten.typeset.element.Box;

import java.util.List;

/**
 * Interface for anything that has width, height, and depth.
 */
public interface Dimensions {
    /**
     * The width of the object in scaled points.
     */
    long getWidth();

    /**
     * The distance between the baseline and the top of the object in scaled points.
     */
    long getHeight();

    /**
     * The distance between the baseline and the bottom of the object in scaled points.
     */
    long getDepth();

    /**
     * Computes the sum of the item widths and the max of the height and depth.
     */
    static Dimensions horizontally(List<? extends Dimensions> items) {
        long totalWidth = 0;
        long totalHeight = 0;
        long totalDepth = 0;

        for (Dimensions item : items) {
            long width = item.getWidth();
            long height = item.getHeight();
            long depth = item.getDepth();

            totalWidth += width;
            totalHeight = Math.max(totalHeight, height);
            totalDepth = Math.max(totalDepth, depth);
        }

        return new AbstractDimensions(totalWidth, totalHeight, totalDepth);
    }

    /**
     * Compute the dimensions of a vertical stack of elements, aligning them according to the specified parameter.
     */
    static Dimensions vertically(List<? extends Dimensions> elements, VerticalAlignment verticalAlignment) {
        Dimensions dimensions;

        switch (verticalAlignment) {
            case TOP:
                throw new IllegalArgumentException();

            case FIRST_BOX:
                dimensions = verticallyFirstBoxAligned(elements);
                break;

            case LAST_BOX:
                dimensions = verticallyLastBoxAligned(elements);
                break;

            case BOTTOM:
            default:
                throw new IllegalArgumentException();
        }

        return dimensions;
    }

    /**
     * Computes the size of a vertical box where the baseline alignment is on the first box.
     * The width is the max of the widths. The height is everything above the first box and the
     * height of the first box. The depth is the rest.
     *
     * @param elements the elements top to bottom.
     */
    static Dimensions verticallyFirstBoxAligned(List<? extends Dimensions> elements) {
        long boxWidth = 0;
        long boxHeight = 0;
        long boxDepth = 0;
        boolean sawBox = false;

        for (Dimensions element : elements) {
            long width = element.getWidth();
            long height = element.getHeight();
            long depth = element.getDepth();

            boxWidth = Math.max(boxWidth, width);

            // Once we've seen a box, everything else is depth.
            if (sawBox) {
                boxDepth += height + depth;
            } else {
                boxHeight += height + depth;
            }

            // For our first box, transfer the depth to the box depth.
            if (!sawBox && element instanceof Box) {
                boxHeight -= depth;
                boxDepth = depth;
                sawBox = true;
            }
        }

        return new AbstractDimensions(boxWidth, boxHeight, boxDepth);
    }

    /**
     * Computes the size of a vertical box where the baseline alignment is on the last box.
     * The width is the max of the widths. The height is the sum of all the heights and depths
     * except for the last box's depth. The depth is the last box's depth and all glues after that.
     *
     * @param elements the elements top to bottom.
     */
    static Dimensions verticallyLastBoxAligned(List<? extends Dimensions> elements) {
        long boxWidth = 0;
        long boxHeight = 0;
        long boxDepth = 0;
        boolean sawBox = false;

        for (Dimensions element : elements) {
            long width = element.getWidth();
            long height = element.getHeight();
            long depth = element.getDepth();

            boxWidth = Math.max(boxWidth, width);

            // Once we've seen a box, everything else is depth.
            if (sawBox) {
                boxDepth += height + depth;
            } else {
                boxHeight += height + depth;
            }

            // ... until we see another box, and the depth is transferred to the height.
            if (element instanceof Box) {
                boxHeight += boxDepth - depth;
                boxDepth = depth;
                sawBox = true;
            }
        }

        return new AbstractDimensions(boxWidth, boxHeight, boxDepth);
    }
}

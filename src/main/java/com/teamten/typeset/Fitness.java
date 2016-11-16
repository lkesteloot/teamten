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

import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Flexibility;
import com.teamten.typeset.element.Flexible;
import com.teamten.typeset.element.HBox;
import com.teamten.typeset.element.Text;
import com.teamten.typeset.element.TotalFlexibility;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents how well a set of elements can fit into a space.
 */
public class Fitness {
    private static final long INFINITELY_BAD = 100_000;
    private final long mExtraSpace;
    private final long mSize;
    private final Flexibility mStretch;
    private final Flexibility mShrink;
    private final double mRatio;
    private final boolean mRatioIsInfinite;
    private final boolean mIsFlexible;
    private final boolean mIsOverfull;

    private Fitness(long extraSpace, long size, Flexibility stretch, Flexibility shrink,
                    double ratio, boolean ratioIsInfinite,
                    boolean isFlexible, boolean isOverfull) {

        mExtraSpace = extraSpace;
        mSize = size;
        mStretch = stretch;
        mShrink = shrink;
        mRatio = ratio;
        mRatioIsInfinite = ratioIsInfinite;
        mIsFlexible = isFlexible;
        mIsOverfull = isOverfull;
    }

    public long getExtraSpace() {
        return mExtraSpace;
    }

    public long getSize() {
        return mSize;
    }

    public double getRatio() {
        return mRatio;
    }

    public boolean isOverfull() {
        return mIsOverfull;
    }

    /**
     * Create a new Fitness object given a list of elements and a size they must fit into.
     *
     * @param actualSize the total size of the elements. Use -1 to have this method compute it.
     * @param elementSizer returns the size of an element.
     */
    public static Fitness create(List<Element> elements, long maxSize, long actualSize, ElementSizer elementSizer) {
        // Find the sum of the sizes of all the elements in this line or page. Also compute the total stretch
        // and shrink for the glue in that line or page.
        long size = 0;
        TotalFlexibility stretch = new TotalFlexibility();
        TotalFlexibility shrink = new TotalFlexibility();
        for (Element element : elements) {
            size += elementSizer.getElementSize(element);

            // Sum up the stretch and shrink for glues and other flexible objects.
            if (element instanceof Flexible) {
                Flexible flexible = (Flexible) element;
                stretch.add(flexible.getStretch());
                shrink.add(flexible.getShrink());
            }
        }

        // Override with actual size if provided.
        if (actualSize != -1) {
            size = actualSize;
        }

        // Compute difference between width and page width (or height and page height). This is positive
        // if our line comes short and leaves extra space.
        long extraSpace = maxSize - size;

        // See whether we're short or long.
        double ratio;
        boolean ratioIsInfinite;
        boolean isStretchable = true;
        boolean isOverfull = false;
        if (extraSpace > 0) {
            // Our line is short. Compute how much we'd have to stretch.
            if (stretch.getAmount() > 0) {
                // Can stretch, figure out by how much.
                ratio = extraSpace / (double) stretch.getAmount();
                ratioIsInfinite = stretch.isInfinite();
            } else {
                // There's no glue to stretch.
                ratio = 0;
                ratioIsInfinite = false;
                isStretchable = false;
            }
        } else if (extraSpace < 0) {
            // Our line is long. Compute how much we'd have to shrink.
            if (!shrink.isInfinite() && -extraSpace > shrink.getAmount()) {
                // Can't shrink more than shrink amount.
                ratio = -1.0;
                ratioIsInfinite = false;
                isOverfull = true;
            } else if (shrink.getAmount() > 0) {
                // This will be negative.
                ratio = extraSpace / (double) shrink.getAmount();
                ratioIsInfinite = shrink.isInfinite();
            } else {
                // There's no glue to shrink.
                ratio = 0;
                ratioIsInfinite = false;
                isStretchable = false;
            }
        } else {
            // Our line is just right.
            ratio = 0;
            ratioIsInfinite = false;
        }

        return new Fitness(extraSpace, size, stretch.toFlexibility(), shrink.toFlexibility(),
                ratio, ratioIsInfinite, isStretchable, isOverfull);
    }


    /**
     * Given what we found about this line or page, compute the badness, which basically tells us how
     * much we had to stretch or shrink.
     */
    public long computeBadness() {
        long badness;

        if (mRatioIsInfinite) {
            // No badness for infinite stretch or shrink.
            badness = 0;
        } else if (mIsOverfull) {
            // We're overfull. This is infinitely bad.
            badness = INFINITELY_BAD;
        } else if (!mIsFlexible) {
            // We don't match the right size and we can't stretch or shrink. This is infinitely bad.
            badness = INFINITELY_BAD;
        } else {
            // Normal case. Use 100*r^3, but max out at INFINITELY_BAD.
            if (mRatio < -10 || mRatio > 10) {
                // Avoid overflow. 10 = ceil((INFINITELY_BAD/100)^(1/3)).
                badness = INFINITELY_BAD;
            } else {
                badness = Math.min(INFINITELY_BAD, (long) (100 * Math.pow(Math.abs(mRatio), 3)));
            }
        }

        return badness;
    }

    /**
     * Returns a copy of the element list but with the flexible element fixed so that all the elements
     * will fit properly in the size that was specified when this object was created.
     */
    public List<Element> fixed(List<Element> elements) {
        List<Element> fixedElements = new ArrayList<>();

        // Non-null iff the previous element was a Text element.
        Text previousText = null;

        for (Element element : elements) {
            if (element instanceof HBox) {
                HBox hbox = (HBox) element;
                if (hbox.isEmpty()) {
                    // We can get this as a result of choosing a part of a discretionary that was empty.
                    // Suppress them altogether so that they don't interfere with our text concatenation scheme.
                    element = null;
                }
            }
            if (element instanceof Flexible) {
                Flexible flexible = (Flexible) element;

                long glueSize = flexible.getSize();
                Flexibility flexibility = mRatio >= 0 ? flexible.getStretch() : flexible.getShrink();
                if (flexibility.isInfinite() == mRatioIsInfinite) {
                    glueSize += (long) (flexibility.getAmount() * mRatio);
                }

                // Fix the glue.
                element = flexible.fixed(glueSize);
            }

            // Combine consecutive Text elements.
            if (element instanceof Text) {
                Text text = (Text) element;

                // See if we can combine with previous text.
                if (previousText == null) {
                    previousText = text;
                } else {
                    if (text.isCompatibleWith(previousText)) {
                        // Combine with previous text and get rid of this one.
                        previousText = previousText.appendedWith(text);
                    } else {
                        // New text is not compatible. Output old text.
                        fixedElements.add(previousText);
                        previousText = text;
                    }
                }

                // Suppress current element.
                element = null;
            } else if (previousText != null && element != null) {
                // Not text, flush the previous text.
                fixedElements.add(previousText);
                previousText = null;
            }

            if (element != null) {
                fixedElements.add(element);
            }
        }

        if (previousText != null) {
            fixedElements.add(previousText);
            previousText = null;
        }

        return fixedElements;
    }

    /**
     * Functional interface for getting the size of an element.
     */
    @FunctionalInterface
    public interface ElementSizer {
        /**
         * Returns the size of the element in the appropriate dimension, depending on how this Fitness
         * is being computed.
         */
        long getElementSize(Element element);
    }
}

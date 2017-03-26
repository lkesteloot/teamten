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
import com.teamten.typeset.element.Footnote;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.HBox;
import com.teamten.typeset.element.Image;
import com.teamten.typeset.element.Rule;
import com.teamten.typeset.element.Text;
import com.teamten.typeset.element.TotalFlexibility;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.IN;
import static com.teamten.typeset.SpaceUnit.PC;
import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents a sequence of elements being considered for fitting into a space, either horizontally
 * into a line or vertically into a page.
 */
public class Chunk {
    private static final long INFINITELY_BAD = 100_000;
    private final List<Element> mElements;
    private final List<Image> mImages;
    private final List<Footnote> mFootnotes;
    private final long mExtraSpace;
    private final long mSize;
    private final double mRatio;
    private final boolean mRatioIsInfinite;
    private final boolean mIsFlexible;
    private final boolean mIsOverfull;

    private Chunk(List<Element> elements,
                  List<Image> images,
                  List<Footnote> footnotes,
                  long extraSpace, long size,
                  double ratio, boolean ratioIsInfinite,
                  boolean isFlexible, boolean isOverfull) {

        mElements = elements;
        mImages = images;
        mFootnotes = footnotes;
        mExtraSpace = extraSpace;
        mSize = size;
        mRatio = ratio;
        mRatioIsInfinite = ratioIsInfinite;
        mIsFlexible = isFlexible;
        mIsOverfull = isOverfull;
    }

    /**
     * The list of images that were extracted from the original list of elements. If there were none,
     * this returns an empty list.
     */
    public List<Image> getImages() {
        return mImages;
    }

    /**
     * The list of footnotes that were extracted from the original list of elements. If there were none,
     * this returns an empty list.
     */
    public List<Footnote> getFootnotes() {
        return mFootnotes;
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
     * Create a new Chunk object given a list of elements and a size they must fit into.
     *
     * @param actualSize the total size of the elements. Use -1 to have this method compute it.
     * @param extractImagesAndFootnotes whether to pull images and footnotes into the separate {@code images}
     * and {@code footnotes} properties.
     * @param elementSizer returns the size of an element.
     */
    public static Chunk create(List<Element> elements, long maxSize, long actualSize,
                               boolean extractImagesAndFootnotes,
                               boolean placeFootnotes, ElementSizer elementSizer) {

        // See if our list has any images or footnotes. These are never supposed to be in-line.
        boolean hasImageOrFootnote = false;
        if (extractImagesAndFootnotes) {
            for (Element element : elements) {
                if (element instanceof Image || element instanceof Footnote) {
                    hasImageOrFootnote = true;
                    break;
                }
            }
        }

        // If we had any images or footnotes, pull them out into a separate list.
        List<Image> images;
        List<Footnote> footnotes;
        if (hasImageOrFootnote) {
            // Don't modify the original, it might be used again by the caller.
            List<Element> newElements = new ArrayList<>(elements.size());
            images = new ArrayList<>();
            footnotes = new ArrayList<>();
            for (Element element : elements) {
                if (element instanceof Image) {
                    images.add((Image) element);
                } else if (element instanceof Footnote) {
                    footnotes.add((Footnote) element);
                } else {
                    newElements.add(element);
                }
            }
            // Replace original.
            elements = newElements;
        } else {
            images = Collections.emptyList();
            footnotes = Collections.emptyList();
        }

        // Add the footnotes to the bottom.
        if (placeFootnotes && !footnotes.isEmpty()) {
            elements.add(new Glue(PC.toSp(1), PT.toSp(1), 0, false));
            elements.add(new Rule(IN.toSp(0.5), PT.toSp(0.5), 0));
            elements.add(new Glue(PC.toSp(1), 0, 0, false));

            // Add all the footnotes, putting space between them to get the right baseline skip.
            Footnote previousFootnote = null;
            for (Footnote footnote : footnotes) {
                // Add space between footnotes.
                if (previousFootnote != null) {
                    long skip = Math.max(0, previousFootnote.getBaselineSkip() -
                            previousFootnote.getLastHBoxDepth() - footnote.getFirstHBoxHeight());
                    if (skip > 0) {
                        elements.add(new Glue(skip, 0, 0, false));
                    }
                }
                elements.add(footnote);
                previousFootnote = footnote;
            }
        }

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
                ratio = extraSpace/(double) stretch.getAmount();
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
                ratio = extraSpace/(double) shrink.getAmount();
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

        return new Chunk(elements, images, footnotes, extraSpace, size,
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
                badness = Math.min(INFINITELY_BAD, (long) (100*Math.pow(Math.abs(mRatio), 3)));
            }
        }

        return badness;
    }

    /**
     * Returns a copy of the element list but with the flexible element fixed so that all the elements
     * will fit properly in the size that was specified when this object was created.
     */
    public List<Element> fixed() {
        List<Element> fixedElements = new ArrayList<>();

        // Non-null iff the previous element was a Text element.
        Text previousText = null;

        for (Element element : mElements) {
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
                    glueSize += (long) (flexibility.getAmount()*mRatio);
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
         * Returns the size of the element in the appropriate dimension, depending on how this Chunk
         * is being computed.
         */
        long getElementSize(Element element);
    }
}

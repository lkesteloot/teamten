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

import com.google.common.collect.Lists;
import com.teamten.typeset.element.Box;
import com.teamten.typeset.element.Discretionary;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Footnote;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.Image;
import com.teamten.typeset.element.NonDiscardableElement;
import com.teamten.typeset.element.Penalty;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Accumulates elements in a list, then finds the best place to break that list to make either lines
 * or pages.
 */
public abstract class ElementList implements ElementSink {
    private static final long LINE_PENALTY = 10;
    private static final long BADNESS_TOLERANCE = 8_000;
    private static final boolean DEBUG_HORIZONTAL_LIST = false;
    private static final boolean DEBUG_VERTICAL_LIST = false;
    private final List<Element> mElements = new ArrayList<>();

    @Override
    public void addElement(Element element) {
        mElements.add(element);
    }

    public List<Element> getElements() {
        return mElements;
    }

    /**
     * Whether to print debugging information.
     */
    @Contract(pure = true)
    private boolean printDebug() {
        return (DEBUG_HORIZONTAL_LIST && this instanceof HorizontalList) ||
                (DEBUG_VERTICAL_LIST && this instanceof VerticalList);
    }

    /**
     * Get a string that can be used for debugging to represent the paragraph starting here.
     *
     * @param beginBreakpoint the start of the line.
     * @param endBreakpoint the end of the line.
     */
    @NotNull
    private String getDebugLinePrefix(Breakpoint beginBreakpoint, Breakpoint endBreakpoint) {
        StringBuilder builder = new StringBuilder();

        for (int i = beginBreakpoint.getStartIndex(); i < endBreakpoint.getIndex() && builder.length() < 20; i++) {
            Element element = mElements.get(i);
            builder.append(element.toTextString());
        }

        builder.append("...");

        return builder.toString();
    }

    /**
     * Get a string that can be used for debugging to represent the paragraph ending here.
     *
     * @param endBreakpoint the end of the line.
     */
    @NotNull
    private String getDebugLineSuffix(Breakpoint endBreakpoint) {
        StringBuilder builder = new StringBuilder();

        for (int i = endBreakpoint.getIndex() - 1; i >= 0 && builder.length() < 20; i--) {
            Element element = mElements.get(i);
            builder.insert(0, element.toTextString());
        }

        builder.insert(0, "...");

        return builder.toString();
    }

    /**
     * Format the list and add the elements to the sink. For horizontal lists this makes paragraphs,
     * and for vertical lists this makes pages.
     */
    public void format(ElementSink output, long maxSize) {
        format(output, OutputShape.fixed(maxSize));
    }

    /**
     * Format the list and add the elements to the sink. For horizontal lists this makes paragraphs,
     * and for vertical lists this makes pages.
     *
     * @param outputShape the shape of the output paragraph of pages.
     */
    public void format(ElementSink output, OutputShape outputShape) {
        // Find all the places that we could break a line or page.
        List<Breakpoint> breakpoints = findBreakpoints();

        // For each possible breakpoint, figure out the next displayed element (after the breakpoint).
        computeStartIndices(breakpoints);

        // Keep track of a list of active breakpoints. As we march through the list of all breakpoints,
        // the active ones will represent the possible start of lines or pages. Breakpoints will be
        // deactivated once the line or page becomes too long. Use a linked list so that we can cheaply
        // delete from the front or the middle when deactivating breakpoints.
        List<Breakpoint> activeBreakpoints = new LinkedList<>();

        // At first only the first breakpoint is active. It's the artificial one before the first element.
        // See the construction of the breakpoints list in findBreakpoints().
        activeBreakpoints.add(breakpoints.get(0));

        // Go through all breakpoints, considering them as possible ends of lines or pages. Skip the first one
        // since it's not a possible end of line.
        for (Breakpoint endBreakpoint : breakpoints.subList(1, breakpoints.size())) {
            if (printDebug()) {
                System.out.printf("Ending at element %d (%s)\n",
                        endBreakpoint.getIndex(), getDebugLineSuffix(endBreakpoint));
            }

            // Go through all active breakpoints, considering them as start of the line or page. We'll find the
            // one that results in the best paragraph or page ending at endBreakpoint. To do that we'll compute the
            // demerits of this line/page, and add that to the sum of the demerits ending at the beginBreakpoint.

            // Initialize the current (end) breakpoint with max values.
            endBreakpoint.setTotalDemerits(Long.MAX_VALUE);
            endBreakpoint.setPreviousBreakpoint(null, 0);

            // We use an iterator so that we can easily and efficiently remove breakpoints that are too far away.
            Iterator<Breakpoint> itr = activeBreakpoints.iterator();
            while (itr.hasNext()) {
                Breakpoint beginBreakpoint = itr.next();

                // Some penalty elements are marked as only valid on even pages. Here we check if it's the
                // case, and if we're on an odd page, completely ignore it.
                if (shouldIgnoreBreakpoint(beginBreakpoint, endBreakpoint)) {
                    continue;
                }

                // Get the counter and size for this line or page.
                int counter = beginBreakpoint.getCounter() + 1;
                long maxSize = outputShape.getSize(counter);

                // Compute the chunk of this sublist.
                Chunk chunk = Chunk.create(getElementSublist(beginBreakpoint, endBreakpoint), maxSize, -1,
                        true, this instanceof VerticalList, this::getElementSize);

                // Compute badness for the line. This is based on how much we had to stretch or shrink.
                long badness = chunk.computeBadness();

                // Get the penalty for the break at the end of this line.
                long penalty = endBreakpoint.getPenalty();

                // Don't consider breaks if the badness exceeds our tolerance. We'll accept really bad lines
                // if we've not found a break at all and we're overfull (to allow lines to break when a very
                // long unbreakable word crosses the boundary). We'll also allow it if we're being forced
                // to break here by a penalty.
                if (badness <= BADNESS_TOLERANCE ||
                        (endBreakpoint.getPreviousBreakpoint() == null && chunk.isOverfull()) ||
                        penalty == -Penalty.INFINITY) {

                    // Compute demerits for this line.
                    long demerits = LINE_PENALTY + badness;
                    demerits = demerits*demerits;

                    // Square the penalty (keeping the sign).
                    if (penalty > -Penalty.INFINITY) {
                        demerits += Long.signum(penalty)*penalty*penalty;
                    } else {
                        // No point in adding a constant to a line we know we're going to break anyway.
                    }

                    // Add the demerits for the paragraph or page ending at the beginning breakpoint.
                    long totalDemerits = demerits + beginBreakpoint.getTotalDemerits();

                    if (printDebug()) {
                        System.out.printf("  from element %d (%s): %.1f - %.1f = %.1f (b = %,d, d = %,d, total d = %,d, r = %.3f)%n",
                                beginBreakpoint.getStartIndex(), getDebugLinePrefix(beginBreakpoint, endBreakpoint),
                                PT.fromSp(maxSize), PT.fromSp(chunk.getSize()), PT.fromSp(chunk.getExtraSpace()),
                                badness, demerits, totalDemerits, chunk.getRatio());
                    }

                    // If it's the best we've seen so far, remember it.
                    if (totalDemerits < endBreakpoint.getTotalDemerits()) {
                        // Figure out how much to increase the counter for this particular case.
                        // Add one because we have to include this chunk.
                        int increment = getChunkExtraIncrement(chunk) + 1;
                        endBreakpoint.setPreviousBreakpoint(beginBreakpoint, increment);
                        endBreakpoint.setChunk(chunk);
                        endBreakpoint.setTotalDemerits(totalDemerits);

                        if (printDebug()) {
                            System.out.println("    ^^^ best so far");
                        }
                    }
                } else {
                    // This element's badness exceeded our tolerance. Log it.
                    if (printDebug()) {
                        System.out.printf("  ignored element %d (%s): %.1f - %.1f = %.1f (b = %,d, r = %.3f)%n",
                                beginBreakpoint.getStartIndex(), getDebugLinePrefix(beginBreakpoint, endBreakpoint),
                                PT.fromSp(maxSize), PT.fromSp(chunk.getSize()), PT.fromSp(chunk.getExtraSpace()),
                                badness, chunk.getRatio());
                    }
                }

                // If we're overfull, then deactivate this breakpoint, since it'll be overfull for all
                // subsequent end breakpoints too. TODO but what if they need it because they can't break?
                if (chunk.isOverfull()) {
                    if (printDebug()) {
                        System.out.println("    XXX removing from active list");
                    }
                    itr.remove();
                }
            }

            // If we found a good line or page for this end breakpoint, then add it as an active breakpoint.
            if (endBreakpoint.getPreviousBreakpoint() != null) {
                // If we're a forced break, then delete all existing active breakpoints. They can't be reached
                // from any breakpoint after us anyway.
                if (endBreakpoint.getPenalty() == -Penalty.INFINITY) {
                    if (printDebug()) {
                        System.out.println("  !!! clearing all active breakpoints");
                    }
                    activeBreakpoints.clear();
                }

                if (printDebug()) {
                    System.out.println("  +++ adding to active list");
                }
                activeBreakpoints.add(endBreakpoint);
            }
        }

        // Convert the final list of breakpoints into boxes.
        Iterable<Box> boxes = makeAllFinalBoxes(breakpoints, outputShape);

        // Send the boxes to the sink.
        outputBoxes(boxes, output, outputShape);
    }

    /**
     * Find all the places that we could break a line or page.
     */
    protected List<Breakpoint> findBreakpoints() {
        List<Breakpoint> breakpoints = new ArrayList<>();

        // For convenience put a breakpoint at the very beginning, at the first element.
        breakpoints.add(new Breakpoint(0, 0));

        for (int i = 0; i < mElements.size(); i++) {
            Element element = mElements.get(i);
            Element previousElement = i == 0 ? null : mElements.get(i - 1);

            if (element instanceof Penalty) {
                // Can break at penalties as long as they're not positive infinity.
                Penalty penalty = (Penalty) element;
                if (penalty.getPenalty() < Penalty.INFINITY) {
                    breakpoints.add(new Breakpoint(i, penalty.getPenalty()));
                }
            } else if (element instanceof Glue && previousElement instanceof NonDiscardableElement) {
                // Can break at glues that are preceded by non-discardable elements.
                breakpoints.add(new Breakpoint(i, 0));
            } else if (element instanceof Discretionary) {
                // Can break at discretionary elements (hyphenation).
                Discretionary discretionary = (Discretionary) element;
                breakpoints.add(new Breakpoint(i, discretionary.getPenalty()));
            }
        }

        // The way we construct paragraphs and pages, there's always a forced breakpoint at the very end, so we don't
        // manually add one.

        return breakpoints;
    }

    /**
     * For each possible breakpoint, figure out the next displayed element (after the breakpoint). Modifies
     * the breakpoint objects.
     */
    protected void computeStartIndices(List<Breakpoint> breakpoints) {
        // Analyze every breakpoint.
        for (int breakpointIndex = 0; breakpointIndex < breakpoints.size(); breakpointIndex++) {
            Breakpoint breakpoint = breakpoints.get(breakpointIndex);
            int index = breakpoint.getIndex();
            int nextIndex = breakpointIndex + 1 < breakpoints.size() ?
                    breakpoints.get(breakpointIndex + 1).getIndex() : mElements.size();

            // See where the line would start by skipping discardable elements.
            while (index < nextIndex && mElements.get(index).shouldSkipElementAtStart()) {
                // Skip this element.
                index++;
            }
            breakpoint.setStartIndex(index);
        }
    }

    /**
     * Whether this end breakpoint should be ignored altogether in this case.
     */
    private boolean shouldIgnoreBreakpoint(Breakpoint beginBreakpoint, Breakpoint endBreakpoint) {
        // Get the element that we're proposing to break on.
        Element element = mElements.get(endBreakpoint.getIndex());

        // See if it's a penalty
        if (element instanceof Penalty) {
            Penalty penalty = (Penalty) element;
            if (penalty.isEvenPageOnly()) {
                // Okay, check if we're on an even page.
                int page = beginBreakpoint.getCounter() + 1;
                if (page%2 != 0) {
                    // We're on an odd page, ignore the penalty that's for even pages only.
                    return true;
                }
            }
        }

        // Don't skip anything else.
        return false;
    }

    /**
     * Go through our linked list of breakpoints, generating lines or pages. The list runs backward through
     * the data, so we build it up into a list in reverse order.
     */
    private Iterable<Box> makeAllFinalBoxes(List<Breakpoint> breakpoints, OutputShape outputShape) {
        Deque<Box> boxes = new ArrayDeque<>();

        Breakpoint endBreakpoint = breakpoints.get(breakpoints.size() - 1);
        while (true) {
            Breakpoint beginBreakpoint = endBreakpoint.getPreviousBreakpoint();
            if (beginBreakpoint == null) {
                // Done, reached beginning.
                break;
            }

            Chunk chunk = endBreakpoint.getChunk();

            // Get the indent and size for this line or page.
            int counter = endBreakpoint.getCounter();
            long indent = outputShape.getIndent(counter);
            long size = outputShape.getSize(counter);

            // See if we got any images.
            List<Image> images = chunk.getImages();
            if (!images.isEmpty()) {
                if (this instanceof HorizontalList) {
                    // Move them right after this line.
                    Lists.reverse(images).forEach(boxes::addFirst);
                } else if (this instanceof VerticalList) {
                    // Create new pages for the images.
                    // Must go backward since we want them in the right order and we're inserting at the front.
                    for (Image image : Lists.reverse(images)) {
                        //  TODO Perhaps this should be moved to VerticalList.
                        List<Element> imagePage = new ArrayList<>();
                        imagePage.add(Glue.infiniteVertical());
                        imagePage.add(image);
                        Box caption = image.getCaption();
                        if (caption != null) {
                            imagePage.add(Glue.vertical(PT.toSp(8.0)));
                            imagePage.add(caption);
                        }
                        imagePage.add(Glue.infiniteVertical());
                        Chunk imageChunk = Chunk.create(imagePage, size, -1, false, false, this::getElementSize);
                        imagePage = imageChunk.fixed();
                        boxes.addFirst(makeOutputBox(imagePage, counter, 0));
                        counter--;
                    }
                }
            }

            // See if we got any footnotes.
            List<Footnote> footnotes = chunk.getFootnotes();
            if (!footnotes.isEmpty()) {
                if (this instanceof HorizontalList) {
                    // Move them right after this line.
                    footnotes.forEach(boxes::addFirst);
                } else if (this instanceof VerticalList) {
                    // They're fine, they're at the bottom of the page.
                }
            }

            // Make a new list with the glue set to specific widths.
            List<Element> fixedElements = chunk.fixed();

            // Add indent. We used to do this with a shift, but shifts aren't taken into account
            // when computing the dimensions of the vertical boxes.
            if (indent > 0) {
                List<Element> indentedElements = new ArrayList<>(fixedElements.size() + 1);
                indentedElements.add(new Box(indent, 0, 0));
                indentedElements.addAll(fixedElements);
                fixedElements = indentedElements;
            }

            // Pack them into a single box.
            Box box = makeOutputBox(fixedElements, counter, 0);
            if (printDebug()) {
                box.println(System.out, "");
            }

            // Push it on the front of the list, so that we'll get it out backwards.
            boxes.addFirst(box);

            // Jump to previous line or page.
            endBreakpoint = beginBreakpoint;
        }

        return boxes;
    }

    /**
     * Output all the boxes to the sink, warning if they're the wrong size.
     */
    private void outputBoxes(Iterable<Box> boxes, ElementSink output, OutputShape outputShape) {
        int counter = 1;
        for (Box box : boxes) {
            long maxSize = outputShape.getIndent(counter) + outputShape.getSize(counter);

            // See if it's the right size.
            long boxSize = getElementSize(box);
            if (boxSize != maxSize) {
                long difference = boxSize - maxSize;
                double percentOff = difference*100.0/maxSize;
                if (percentOff < -1 || percentOff > 1) {
                    System.out.printf("  Warning: %s is of wrong size (should be %,d but is %,d, off by %,d or %.3f%%)\n",
                            box/*.getClass().getSimpleName()*/, maxSize, boxSize, difference, percentOff);
                    String boxString = box.toTextString().trim();
                    if (!boxString.isEmpty()) {
                        System.out.printf("    %s\n", boxString);
                    }
                }
            }

            // Add to the sink. For vertical lists this will also add the inter-line glue.
            output.addElement(box);

            counter++;
        }
    }

    /**
     * Pretty prints the list to the PrintWriter with the given indent. The method must print its own newline.
     */
    public void println(PrintStream stream, String indent) {
        stream.println(this.getClass().getSimpleName() + ":");
        for (Element element : mElements) {
            element.println(stream, indent + "    ");
        }
    }

    /**
     * Generate the Box that will be sent to the output.
     *
     * @param elements the elements of the box.
     * @param counter the number of the box, starting from 1. For horizontal lists this is the line number
     * of the paragraph. For vertical lists this is the physical page number.
     * @param shift how much to shift the box in the final layout (up or to the right).
     */
    protected abstract Box makeOutputBox(List<Element> elements, int counter, long shift);

    /**
     * Return the size (width for horizontal lists, height + depth for vertical lists) of the element.
     */
    protected abstract long getElementSize(Element element);

    /**
     * Return the list of elements on this line or page. Can process elements to make them more appropriate
     * to this particular range.
     */
    protected abstract List<Element> getElementSublist(Breakpoint beginBreakpoint, Breakpoint endBreakpoint);

    /**
     * When generating breakpoints, if there are images and other things in a chunk, we must adjust the counter
     * if those images will take up space (such as a space).
     */
    protected abstract int getChunkExtraIncrement(Chunk chunk);

    /**
     * Keeps track of possible breakpoints in our paragraph or page, their penalty, and their effects on the
     * whole paragraph or page.
     */
    protected static class Breakpoint {
        private final int mIndex;
        private final long mPenalty;
        private int mCounter;
        private int mStartIndex;
        private long mTotalDemerits;
        private Breakpoint mPreviousBreakpoint;
        private Chunk mChunk;

        private Breakpoint(int index, long penalty) {
            mIndex = index;
            mPenalty = penalty;
            mCounter = 0;
            mStartIndex = 0;
            mTotalDemerits = 0;
            mPreviousBreakpoint = null;
            mChunk = null;
        }

        /**
         * The index into the mElements array where this break would happen.
         */
        public int getIndex() {
            return mIndex;
        }

        /**
         * The penalty for breaking here.
         */
        public long getPenalty() {
            return mPenalty;
        }

        /**
         * Generic counter, used for line number or physical page number. The counter of a section
         * is stored in the breakpoint at its end (end of line or end of page).
         */
        public int getCounter() {
            return mCounter;
        }

        /**
         * The index into mElements where the line would start after this break.
         */
        public int getStartIndex() {
            return mStartIndex;
        }

        /**
         * Set the index into mElements where the line would start after this break.
         */
        public void setStartIndex(int startIndex) {
            mStartIndex = startIndex;
        }

        /**
         * The demerits of the rest of the paragraph or page starting at this break.
         */
        public long getTotalDemerits() {
            return mTotalDemerits;
        }

        /**
         * Set the demerits of the rest of the paragraph or page starting at this break.
         */
        public void setTotalDemerits(long totalDemerits) {
            mTotalDemerits = totalDemerits;
        }

        /**
         * The previous breakpoint in the paragraph or page assuming we're selected as a breakpoint.
         */
        public Breakpoint getPreviousBreakpoint() {
            return mPreviousBreakpoint;
        }

        /**
         * Set the previous breakpoint in the paragraph or page assuming we're selected as a breakpoint.
         * Also updates the counter to be {@code increment} more than the previous breakpoint's counter.
         * If the previous breakpoint is null, the counter is set to {@code increment}
         */
        public void setPreviousBreakpoint(Breakpoint previousBreakpoint, int increment) {
            mPreviousBreakpoint = previousBreakpoint;

            if (mPreviousBreakpoint == null) {
                mCounter = increment;
            } else {
                mCounter = mPreviousBreakpoint.mCounter + increment;
            }
        }

        /**
         * The chunk of the line or page ending at this breakpoint.
         */
        public Chunk getChunk() {
            return mChunk;
        }

        /**
         * Set the chunk of the line or page ending at this breakpoint.
         */
        public void setChunk(Chunk chunk) {
            mChunk = chunk;
        }
    }
}

package com.teamten.typeset;

import com.teamten.typeset.element.Box;
import com.teamten.typeset.element.Discretionary;
import com.teamten.typeset.element.Element;
import com.teamten.typeset.element.Glue;
import com.teamten.typeset.element.HBox;
import com.teamten.typeset.element.NonDiscardableElement;
import com.teamten.typeset.element.Penalty;
import com.teamten.typeset.element.Text;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.PrintStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
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
    private static final long INFINITELY_BAD = 100_000;
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
     * Format the list and add the elements to the sync. For horizontal lists this makes paragraphs,
     * and for vertical lists this makes pages.
     */
    public void format(ElementSink output, long maxSize) {
        format(output, OutputShape.fixed(maxSize));
    }

    /**
     * Format the list and add the elements to the sync. For horizontal lists this makes paragraphs,
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
            // demerits of this line, and add that to the sum of the demerits ending at the beginBreakpoint.

            // Initialize the current (end) breakpoint with max values.
            endBreakpoint.setTotalDemerits(Long.MAX_VALUE);
            endBreakpoint.setPreviousBreakpoint(null);

            // We use an iterator so that we can easily and efficiently remove breakpoints that are too far away.
            Iterator<Breakpoint> itr = activeBreakpoints.iterator();
            while (itr.hasNext()) {
                Breakpoint beginBreakpoint = itr.next();

                // Some penalty elements are marked as only valid on even pages. Here we check if it's the
                // case, and if we're on an odd page, completely ignore it.
                if (shouldIgnoreBreakpoint(beginBreakpoint, endBreakpoint)) {
                    continue;
                }

                // Find the sum of the sizes of all the elements in this line or page. Also compute the total stretch
                // and shrink for the glue in that line or page.
                long width = 0;
                Glue.ExpandabilitySum stretch = new Glue.ExpandabilitySum();
                Glue.ExpandabilitySum shrink = new Glue.ExpandabilitySum();
                for (Element element : getElementSublist(beginBreakpoint, endBreakpoint)) {
                    width += getElementSize(element);

                    // Sum up the stretch and shrink for glues.
                    if (element instanceof Glue) {
                        Glue glue = (Glue) element;
                        stretch.add(glue.getStretch());
                        shrink.add(glue.getShrink());
                    }
                }

                // Get the size for this line or page.
                int counter = beginBreakpoint.getCounter() + 1;
                long maxSize = outputShape.getSize(counter);

                // Compute difference between width and page width (or height and page height). This is positive
                // if our line comes short and leaves extra space.
                long extraSpace = maxSize - width;

                // See whether we're short or long.
                double ratio;
                boolean ratioIsInfinite;
                boolean canStretch = true;
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
                        canStretch = false;
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
                        canStretch = false;
                    }
                } else {
                    // Our line is just right.
                    ratio = 0;
                    ratioIsInfinite = false;
                }

                // Compute badness for the line. This is based on how much we had to stretch or shrink.
                long badness = computeBadness(ratio, ratioIsInfinite, canStretch, isOverfull);

                // Get the penalty for the break at the end of this line.
                long penalty = endBreakpoint.getPenalty();

                // Don't consider breaks if the badness exceeds our tolerance. We'll accept really bad lines
                // if we've not found a break at all and we're overfull (to allow lines to break when a very
                // long unbreakable word crosses the boundary). We'll allow allow it if we're being forced
                // to break here by a penalty.
                if (badness <= BADNESS_TOLERANCE || (endBreakpoint.getPreviousBreakpoint() == null && isOverfull) ||
                        penalty == -Penalty.INFINITY) {

                    // Compute demerits for this line.
                    long demerits = (LINE_PENALTY + badness);
                    demerits = demerits*demerits;

                    // Square the penalty (keeping the sign).
                    if (penalty >= 0) {
                        demerits += penalty * penalty;
                    } else if (penalty > -Penalty.INFINITY) {
                        demerits -= penalty * penalty;
                    } else {
                        // No point in adding constant to a line we know we're going to break anyway.
                    }

                    // Add the demerits for the paragraph or page ending at the beginning breakpoint.
                    long totalDemerits = demerits + beginBreakpoint.getTotalDemerits();

                    if (printDebug()) {
                        System.out.printf("  from element %d (%s): %.1f - %.1f = %.1f (b = %,d, d = %,d, total d = %,d, r = %.3f)%n",
                                beginBreakpoint.getStartIndex(), getDebugLinePrefix(beginBreakpoint, endBreakpoint),
                                PT.fromSp(maxSize), PT.fromSp(width), PT.fromSp(extraSpace),
                                badness, demerits, totalDemerits, ratio);
                    }

                    // If it's the best we've seen so far, remember it.
                    if (totalDemerits < endBreakpoint.getTotalDemerits()) {
                        endBreakpoint.setPreviousBreakpoint(beginBreakpoint);
                        endBreakpoint.setRatio(ratio);
                        endBreakpoint.setRatioIsInfinite(ratioIsInfinite);
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
                                PT.fromSp(maxSize), PT.fromSp(width), PT.fromSp(extraSpace),
                                badness, ratio);
                    }
                }

                // If we're overfull, then deactivate this breakpoint, since it'll be overfull for all
                // subsequent end breakpoints too. TODO but what if they need it because they can't break?
                if (isOverfull) {
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
                // Can break at glues that preceded by non-discardable elements.
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
                if (page % 2 != 0) {
                    // We're on an odd page, ignore the penalty that's for even pages only.
                    return true;
                }
            }
        }

        // Don't skip anything else.
        return false;
    }

    /**
     * Given what we found about this line or page, compute the badness, which basically tells us how
     * much we had to stretch or shrink.
     */
    private long computeBadness(double ratio, boolean ratioIsInfinite, boolean canStretch, boolean isOverfull) {
        long badness;

        if (ratioIsInfinite) {
            // No badness for infinite stretch or shrink.
            badness = 0;
        } else if (isOverfull) {
            // We're overfull. This is infinitely bad.
            badness = INFINITELY_BAD;
        } else if (!canStretch) {
            // We don't match the right size and we can't stretch or shrink. This is infinitely bad.
            badness = INFINITELY_BAD;
        } else {
            // Normal case. Use 100*r^3, but max out at INFINITELY_BAD.
            if (ratio < -10 || ratio > 10) {
                // Avoid overflow. 10 = ceil((INFINITELY_BAD/100)^(1/3)).
                badness = INFINITELY_BAD;
            } else {
                badness = Math.min(INFINITELY_BAD, (long) (100 * Math.pow(Math.abs(ratio), 3)));
            }
        }

        return badness;
    }

    /**
     * Make a box with the specified elements stretched out by the given ratio.
     */
    private Box makeBox(List<Element> lineElements, double ratio, boolean ratioIsInfinite, int counter, long shift) {
        List<Element> line = new ArrayList<>();

        // Non-null iff the previous element was a Text element.
        Text previousText = null;

        for (Element element : lineElements) {
            if (element instanceof HBox) {
                HBox hbox = (HBox) element;
                if (hbox.isEmpty()) {
                    // We can get this as a result of choosing a part of a discretionary that was empty.
                    // Suppress them altogether so that they don't interfere with our text concatenation scheme.
                    element = null;
                }
            }
            if (element instanceof Glue) {
                Glue glue = (Glue) element;

                long glueSize = glue.getSize();
                Glue.Expandability expandability = ratio >= 0 ? glue.getStretch() : glue.getShrink();
                if (expandability.isInfinite() == ratioIsInfinite) {
                    glueSize += (long) (expandability.getAmount() * ratio);
                }

                // Fix the glue.
                element = glue.fixed(glueSize);
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
                        line.add(previousText);
                        previousText = text;
                    }
                }

                // Suppress current element.
                element = null;
            } else if (previousText != null && element != null) {
                // Not text, flush the previous text.
                line.add(previousText);
                previousText = null;
            }

            if (element != null) {
                line.add(element);
            }
        }

        if (previousText != null) {
            line.add(previousText);
            previousText = null;
        }

        return makeOutputBox(line, counter, shift);
    }

    /**
     * Make a box with zero stretching or shrinking.
     */
    Box makeBox(int counter) {
        return makeBox(mElements, 0, false, counter, 0);
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

            // Get the indent for this line or page.
            long indent = outputShape.getIndent(endBreakpoint.getCounter());

            // Make a new list with the glue set to specific widths.
            Box box = makeBox(getElementSublist(beginBreakpoint, endBreakpoint),
                    endBreakpoint.getRatio(), endBreakpoint.isRatioIsInfinite(),
                    endBreakpoint.getCounter(), indent);
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
     * Output all the boxes to the sync, warning if they're the wrong size.
     */
    private void outputBoxes(Iterable<Box> boxes, ElementSink output, OutputShape outputShape) {
        int counter = 1;
        for (Box box : boxes) {
            long maxSize = outputShape.getSize(counter);

            // See if it's the right size.
            long boxSize = getElementSize(box);
            if (boxSize != maxSize) {
                long difference = boxSize - maxSize;
                double percentOff = difference*100.0/maxSize;
                if (percentOff < -0.001 || percentOff > 0.001) {
                    System.out.printf("Warning: %s is of wrong size (should be %,d but is %,d, off by %,d or %.3f%%)\n",
                            box.getClass().getSimpleName(), maxSize, boxSize, difference, percentOff);
                    System.out.printf("    %s\n", box.toTextString());
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
     *                of the paragraph. For vertical lists this is the physical page number.
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
        private double mRatio;
        private boolean mRatioIsInfinite;

        private Breakpoint(int index, long penalty) {
            mIndex = index;
            mPenalty = penalty;
            mCounter = 0;
            mStartIndex = 0;
            mTotalDemerits = 0;
            mPreviousBreakpoint = null;
            mRatio = 0;
            mRatioIsInfinite = false;
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
         * Also updates the counter to be one more than the previous breakpoint's counter.
         */
        public void setPreviousBreakpoint(Breakpoint previousBreakpoint) {
            mPreviousBreakpoint = previousBreakpoint;

            if (mPreviousBreakpoint == null) {
                mCounter = 0;
            } else {
                mCounter = mPreviousBreakpoint.mCounter + 1;
            }
        }

        /**
         * The spread (positive) or shrink (ratio) for the line after this breakpoint.
         */
        public double getRatio() {
            return mRatio;
        }

        /**
         * Set the spread (positive) or shrink (ratio) for the line after this breakpoint.
         */
        public void setRatio(double ratio) {
            mRatio = ratio;
        }

        /**
         * Whether the ratio is for infinite glue only.
         */
        public boolean isRatioIsInfinite() {
            return mRatioIsInfinite;
        }

        /**
         * Set whether the ratio is for infinite glue only.
         */
        public void setRatioIsInfinite(boolean ratioIsInfinite) {
            mRatioIsInfinite = ratioIsInfinite;
        }
    }

}

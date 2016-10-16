package com.teamten.typeset;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Accumulates elements in a list, then finds the best place to break that list to make either lines
 * or pages.
 */
public abstract class ElementList implements ElementSink {
    private static final long LINE_PENALTY = 10;
    private static final long BADNESS_TOLERANCE = 5000;
    private static final long INFINITELY_BAD = 10000;
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
    private boolean printDebug() {
        return (DEBUG_HORIZONTAL_LIST && this instanceof HorizontalList) ||
                (DEBUG_VERTICAL_LIST && this instanceof VerticalList);
    }

    /**
     * Get a string that can be used for debugging to represent the paragraph starting here.
     * @param thisBreakpoint the start of the line.
     */
    private String getDebugLinePrefix(Breakpoint thisBreakpoint) {
        StringBuilder builder = new StringBuilder();

        for (int i = thisBreakpoint.getStartIndex(); i < mElements.size() && builder.length() < 20; i++) {
            Element element = mElements.get(i);
            builder.append(element.toTextString());
        }

        builder.append("...");

        return builder.toString();
    }

    /**
     * Get a string that can be used for debugging to represent the paragraph ending here.
     * @param nextBreakpoint the end of the line.
     */
    private String getDebugLineSuffix(Breakpoint thisBreakpoint, Breakpoint nextBreakpoint) {
        StringBuilder builder = new StringBuilder();

        int thisIndex = thisBreakpoint.getStartIndex();

        for (int i = nextBreakpoint.getIndex() - 1; i >= thisIndex && builder.length() < 20; i--) {
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
        // Find all the places that we could break a line or page.
        List<Breakpoint> breakpoints = findBreakpoints();

        // For each possible breakpoint, figure out the next displayed element (after the breakpoint).
        for (int breakpointIndex = 0; breakpointIndex < breakpoints.size(); breakpointIndex++) {
            Breakpoint breakpoint = breakpoints.get(breakpointIndex);
            int index = breakpoint.getIndex();
            int nextIndex = breakpointIndex + 1 < breakpoints.size() ?
                    breakpoints.get(breakpointIndex + 1).getIndex() : mElements.size();

            // See where the line would start by skipping discardable elements. Don't do this on the very
            // first breakpoint, since we'll want to keep glue at the front of the line that might be needed
            // to center it.
            while (breakpointIndex != 0 && index < nextIndex && mElements.get(index) instanceof DiscardableElement) {
                index++;
            }
            breakpoint.setStartIndex(index);
        }

        // Work backwards through the breakpoints. Use dynamic programming to cache the value of what we've
        // computed so far. The cache is stored in the Breakpoint objects.
        for (int thisBreak = breakpoints.size() - 1; thisBreak >= 0; thisBreak--) {
            Breakpoint thisBreakpoint = breakpoints.get(thisBreak);

            if (printDebug()) {
                System.out.printf("Starting at element %d (%s)\n", thisBreak, getDebugLinePrefix(thisBreakpoint));
            }

            // So now we're pretending that our paragraph or page starts here. We're looking for the
            // best next breakpoint.
            Breakpoint bestNextBreakpoint = null;
            long bestTotalDemerits = Long.MAX_VALUE;
            double bestRatio = 0;
            boolean bestRatioIsInfinite = false;
            int bestNextBreak = -1;
            for (int nextBreak = thisBreak + 1; nextBreak < breakpoints.size(); nextBreak++) {
                Breakpoint nextBreakpoint = breakpoints.get(nextBreak);

                // The first line of our paragraph (or first page of our book) will go from thisIndex (inclusive) to
                // nextIndex (inclusive, but only if it's Discretionary). Find the sum of the sizes of all the
                // elements in that line. Also compute the total stretch and shrink for the glue in that line.
                long width = 0;
                Glue.ExpandabilitySum stretch = new Glue.ExpandabilitySum();
                Glue.ExpandabilitySum shrink = new Glue.ExpandabilitySum();
                for (Element element : getLineElements(thisBreakpoint, nextBreakpoint)) {
                    width += getElementSize(element);

                    // Sum up the stretch and shrink for glues.
                    if (element instanceof Glue) {
                        Glue glue = (Glue) element;
                        stretch.add(glue.getStretch());
                        shrink.add(glue.getShrink());
                    }
                }

                // Compute difference between width and page width or height.
                long extraSpace = maxSize - width;

                // See whether we're short or long.
                double ratio;
                boolean ratioIsInfinite;
                boolean canStretch = true;
                boolean isOverfull = false;
                if (extraSpace > 0) {
                    // Our line is short. Compute how much we'd have to stretch.
                    if (stretch.getAmount() > 0) {
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
                    if (!stretch.isInfinite() && -extraSpace > stretch.getAmount()) {
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
                    if (ratio < -5 || ratio > 5) {
                        // Avoid overflow. 5 = ceil((INFINITELY_BAD/100)^(1/3)).
                        badness = INFINITELY_BAD;
                    } else {
                        badness = Math.min(INFINITELY_BAD, (long) (100 * Math.pow(Math.abs(ratio), 3)));
                    }
                }

                // Get the penalty for the break at the end of this line.
                long penalty = nextBreakpoint.getPenalty();

                // Don't consider breaks at infinity or if the badness exceeds our tolerance.
                if (penalty < Penalty.INFINITY && (badness <= BADNESS_TOLERANCE ||
                        bestNextBreakpoint == null || penalty == -Penalty.INFINITY)) {

                    // Compute demerits for this line.
                    long demerits = (LINE_PENALTY + badness);
                    demerits = demerits*demerits;

                    if (penalty >= 0) {
                        demerits += penalty * penalty;
                    } else if (penalty > -Penalty.INFINITY) {
                        demerits -= penalty * penalty;
                    } else {
                        // No point in adding constant to a line we know we're going to break anyway.
                    }

                    // Add the demerits for the paragraph or page starting at the next breakpoint.
                    long totalDemerits = demerits + nextBreakpoint.getTotalDemerits();

                    if (printDebug()) {
                        System.out.printf("  to element %d (%s): %.1f - %.1f = %.1f (b = %,d, d = %,d, total d = %,d, r = %.3f)%n",
                                nextBreak, getDebugLineSuffix(thisBreakpoint, nextBreakpoint),
                                PT.fromSp(maxSize), PT.fromSp(width), PT.fromSp(extraSpace),
                                badness, demerits, totalDemerits, ratio);
                    }

                    if (bestNextBreakpoint == null || totalDemerits <= bestTotalDemerits || penalty == -Penalty.INFINITY) {
                        bestNextBreakpoint = nextBreakpoint;
                        bestNextBreak = nextBreak;
                        bestTotalDemerits = totalDemerits;
                        bestRatio = ratio;
                        bestRatioIsInfinite = ratioIsInfinite;

                        if (penalty == -Penalty.INFINITY) {
                            // We're forced to break here.
                            break;
                        }
                    }
                }

                // Actually, if we're overfull, then we're done considering this line or page, since adding
                // any more will just make it worse (probably).
                if (isOverfull && bestNextBreakpoint != null) {
                    break;
                }
            }

            if (bestNextBreakpoint != null) {
                if (printDebug()) {
                    System.out.printf("  best next break is %d, total demerits %,d, ratio %.3f%n", bestNextBreak, bestTotalDemerits, bestRatio);
                }
                // Store the total demerits at this breakpoint and link to the next break.
                thisBreakpoint.setTotalDemerits(bestTotalDemerits);
                thisBreakpoint.setNextBreakpoint(bestNextBreakpoint);
                thisBreakpoint.setRatio(bestRatio);
                thisBreakpoint.setRatioIsInfinite(bestRatioIsInfinite);
            }
        }

        // Go through our linked list of breakpoints, generating lines or pages.
        Breakpoint thisBreakpoint = breakpoints.get(0);
        while (true) {
            Breakpoint nextBreakpoint = thisBreakpoint.getNextBreakpoint();
            if (nextBreakpoint == null) {
                // Lines or pages are between breakpoints. Nothing after the last one.
                break;
            }

            // Make a new list with the glue set to specific widths.
            Box box = makeBox(getLineElements(thisBreakpoint, nextBreakpoint),
                    thisBreakpoint.getRatio(), thisBreakpoint.isRatioIsInfinite());

            // See if it's the right size.
            long boxSize = getElementSize(box);
            if (boxSize != maxSize) {
                long difference = boxSize - maxSize;
                double percentOff = difference*100.0/maxSize;
                if (percentOff < -0.001 || percentOff > 0.001) {
                    System.out.printf("Warning: %s is of wrong size (should be %,d but is %,d, off by %,d or %.3f%%)\n",
                            box.getClass().getSimpleName(), maxSize, boxSize, difference, percentOff);
                }
            }

            // Add to the sink.
            output.addElement(box);

            // Next line or page.
            thisBreakpoint = nextBreakpoint;
        }
    }

    /**
     * Find all the places that we could break a line or page.
     */
    private List<Breakpoint> findBreakpoints() {
        List<Breakpoint> breakpoints = new ArrayList<>();

        // For convenience put a breakpoint at the very beginning, at the first element.
        breakpoints.add(new Breakpoint(0, 0));

        for (int i = 0; i < mElements.size(); i++) {
            Element element = mElements.get(i);
            Element previousElement = i == 0 ? null : mElements.get(i - 1);

            if (element instanceof Penalty) {
                // Can break at penalties as long as they're not positive infinity.
                Penalty penalty = (Penalty) element;
                if (penalty.getPenalty() != Penalty.INFINITY) {
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
     * Return the list of elements on this line, from thisBreakpoint (inclusive) to nextBreakpoint
     * (inclusive only if it's a discretionary element). All discretionary elements are turned
     * into HBoxes depending on where they are.
     */
    private List<Element> getLineElements(Breakpoint thisBreakpoint, Breakpoint nextBreakpoint) {
        int thisIndex = thisBreakpoint.getStartIndex();
        int nextIndex = nextBreakpoint.getIndex();

        List<Element> elements = new ArrayList<>(Math.max(nextIndex - thisIndex + 1, 10));

        for (int i = thisIndex; i <= nextIndex; i++) {
            Element element = mElements.get(i);

            // Include all discretionary elements, but convert them to HBoxes.
            if (element instanceof Discretionary) {
                Discretionary discretionary = (Discretionary) element;
                HBox hbox;
                if (i == thisIndex) {
                    // This is the discretionary break at the beginning of the line. Use the "post" HBox.
                    hbox = discretionary.getPostBreak();
                } else if (i == nextIndex) {
                    // This is the discretionary break at the end of the line. Use the "pre" HBox.
                    hbox = discretionary.getPreBreak();
                } else {
                    // This is a discretionary in the middle of the line. Use the "no" HBox.
                    hbox = discretionary.getNoBreak();
                }
                elements.add(hbox);
            } else if (i < nextIndex) {
                elements.add(element);
            }
        }

        return elements;
    }

    /**
     * Make a box with the specified elements stretched out by the given ratio.
     */
    private Box makeBox(List<Element> lineElements, double ratio, boolean ratioIsInfinite) {
        List<Element> line = new ArrayList<>();
        for (Element element : lineElements) {
            if (element instanceof Glue) {
                Glue glue = (Glue) element;

                long glueSize = glue.getSize();
                Glue.Expandability expandability = ratio >= 0 ? glue.getStretch() : glue.getShrink();
                if (expandability.isInfinite() == ratioIsInfinite) {
                    glueSize += (long) (expandability.getAmount() * ratio);
                }

                // Fix the glue.
                element = new Glue(glueSize, 0, 0, glue.isHorizontal());
            }

            line.add(element);
        }

        return makeOutputBox(line);
    }

    /**
     * Make a box with zero stretching or shrinking.
     */
    Box makeBox() {
        return makeBox(mElements, 0, false);
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
     */
    protected abstract Box makeOutputBox(List<Element> elements);

    /**
     * Return the size (width for horizontal lists, height + depth for vertical lists) of the element.
     */
    protected abstract long getElementSize(Element element);

    /**
     * Keeps track of possible breakpoints in our paragraph or page, their penalty, and their effects on the
     * whole paragraph or page.
     */
    private static class Breakpoint {
        private final int mIndex;
        private final long mPenalty;
        private int mStartIndex;
        private long mTotalDemerits;
        private Breakpoint mNextBreakpoint;
        private double mRatio;
        private boolean mRatioIsInfinite;

        private Breakpoint(int index, long penalty) {
            mIndex = index;
            mPenalty = penalty;
            mStartIndex = 0;
            mTotalDemerits = 0;
            mNextBreakpoint = null;
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
         * The next breakpoint in the paragraph or page assuming we're selected as a breakpoint.
         */
        public Breakpoint getNextBreakpoint() {
            return mNextBreakpoint;
        }

        /**
         * Set the next breakpoint in the paragraph or page assuming we're selected as a breakpoint.
         */
        public void setNextBreakpoint(Breakpoint nextBreakpoint) {
            mNextBreakpoint = nextBreakpoint;
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

package com.teamten.typeset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Accumulates elements in a list, then finds the best place to break that list to make either lines
 * or pages.
 */
public abstract class ElementList implements ElementSink {
    private final List<Element> mElements = new ArrayList<>();

    @Override
    public void addElement(Element element) {
        mElements.add(element);
    }

    private boolean printDebug() {
        return this instanceof VerticalList;
    }

    /**
     * Format the horizontal list and add the elements to the vertical list.
     */
    public void format(ElementSink output, long maxSize) {
        // Find all the places that we could break a line.
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
            }
        }

        // The way we construct paragraphs, there's always a forced breakpoint at the very end, so we don't
        // manually add one.

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
        // computed so far.
        for (int thisBreak = breakpoints.size() - 1; thisBreak >= 0; thisBreak--) {
            if (printDebug()) {
                System.out.println("starting at element " + thisBreak);
            }
            Breakpoint thisBreakpoint = breakpoints.get(thisBreak);
            int thisIndex = thisBreakpoint.getStartIndex();

            // So now we're pretending that our paragraph starts here. We're looking for the best next breakpoint.
            Breakpoint bestNextBreakpoint = null;
            long bestBadness = Long.MAX_VALUE;
            double bestRatio = 0;
            boolean bestRatioIsInfinite = false;
            int bestNextBreak = -1; // TODO delete.
            for (int nextBreak = thisBreak + 1; nextBreak < breakpoints.size(); nextBreak++) {
                Breakpoint nextBreakpoint = breakpoints.get(nextBreak);
                int nextIndex = nextBreakpoint.getIndex();

                // The first line of our paragraph will go from thisIndex (inclusive) to nextIndex (exclusive).
                // Find the sum of all the elements in that line. Also compute the total stretch and shrink
                // for the glue in that line.
                long width = 0;
                Glue.ExpandabilitySum stretch = new Glue.ExpandabilitySum();
                Glue.ExpandabilitySum shrink = new Glue.ExpandabilitySum();

                for (int i = thisIndex; i < nextIndex; i++) {
                    Element element = mElements.get(i);

                    // TODO
                    width += (this instanceof HorizontalList) ? element.getWidth() : (element.getHeight() + element.getDepth());

                    // Sum up the stretch and shrink for glues.
                    if (element instanceof Glue) {
                        Glue glue = (Glue) element;
                        stretch.add(glue.getStretch());
                        shrink.add(glue.getShrink());
                    }
                }

                // Start with our own penalty, since we're breaking here.
                long badness = thisBreakpoint.getPenalty();

                // Compute difference between width and page width.
                long difference = maxSize - width;

                // See whether we're short or long.
                double ratio;
                boolean ratioIsInfinite;
                boolean noStretch = false;
                if (difference > 0) {
                    // Our line is short. Compute how much we'd have to stretch.
                    if (stretch.getAmount() > 0) {
                        ratio = difference / (double) stretch.getAmount();
                        ratioIsInfinite = stretch.isInfinite();
                    } else {
                        // There's no glue to stretch.
                        ratio = 0;
                        ratioIsInfinite = false;
                        noStretch = true;
                    }
                } else if (difference < 0) {
                    // Our line is long. Compute how much we'd have to shrink.
                    if (shrink.getAmount() > 0) {
                        // This will be negative.
                        ratio = difference / (double) shrink.getAmount();
                        ratioIsInfinite = shrink.isInfinite();
                    } else {
                        // There's no glue to shrink.
                        ratio = 0;
                        ratioIsInfinite = false;
                        noStretch = true;
                    }
                } else {
                    // Our line is just right, there's no extra badness.
                    ratio = 0;
                    ratioIsInfinite = false;
                }

                // See if the ratio is acceptable.
                if (ratio < -1) {
                    // Can't shrink past maximum shrinkage.
                } else {
                    // Add the badness for the paragraph starting at the next breakpoint.
                    badness += nextBreakpoint.getBadness();

                    if (noStretch) {
                        // Didn't find any place to stretch or shrink. Keep it anyway, but penalize it.
                        if (difference < 0) {
                            // TODO still don't understand the difference between these two cases. Look up
                            // the TeX code.
                            // Overfull box, consider this to be infinitely bad.
                            badness = Long.MAX_VALUE;
                        } else {
                            // Underfull box.
                            badness = Penalty.INFINITY;
                        }
                    } else if (ratioIsInfinite) {
                        // Don't penalize for infinite stretch or shrink.
                    } else {
                        // Factor the ratio into the badness.
                        badness += 100 * Math.pow(Math.abs(ratio), 3);
                    }
                    if (printDebug()) {
                        System.out.printf("  to element %d: difference = %.1f - %.1f = %.1f (badness = %d, bestBadness = %d, ratio = %.3f)%n", nextBreak, PT.fromSp(maxSize), PT.fromSp(width), PT.fromSp(difference),
                                badness, bestBadness, ratio);
                    }

                    if (bestNextBreakpoint == null || badness <= bestBadness) {
                        bestNextBreakpoint = nextBreakpoint;
                        bestNextBreak = nextBreak;
                        bestBadness = badness;
                        bestRatio = ratio;
                        bestRatioIsInfinite = ratioIsInfinite;
                    }
                }
            }

            if (bestNextBreakpoint != null) {
                if (printDebug()) {
                    System.out.printf("  best next break is %d, badness %d, ratio %.3f%n", bestNextBreak, bestBadness, bestRatio);
                }
                // Store the badness at this breakpoint and link to the next break.
                thisBreakpoint.setBadness(bestBadness);
                thisBreakpoint.setNextBreakpoint(bestNextBreakpoint);
                thisBreakpoint.setRatio(bestRatio);
                thisBreakpoint.setRatioIsInfinite(bestRatioIsInfinite);
            }
        }

        // Go through our linked list of breakpoints, generating lines.
        Breakpoint thisBreakpoint = breakpoints.get(0);
        while (true) {
            Breakpoint nextBreakpoint = thisBreakpoint.getNextBreakpoint();
            if (nextBreakpoint == null) {
                // Lines are between breakpoints. Nothing after the last one.
                break;
            }

            // Find indices into the element list.
            int thisIndex = thisBreakpoint.getStartIndex();
            int nextIndex = nextBreakpoint.getIndex();

            // Make a new list with the glue set to specific widths.
            List<Element> line = new ArrayList<>();
            for (Element element : mElements.subList(thisIndex, nextIndex)) {
                if (element instanceof Glue) {
                    Glue glue = (Glue) element;
                    double ratio = thisBreakpoint.getRatio();

                    long glueSize = glue.getSize();
                    Glue.Expandability expandability = ratio >= 0 ? glue.getStretch() : glue.getShrink();
                    if (expandability.isInfinite() == thisBreakpoint.isRatioIsInfinite()) {
                        glueSize += (long) (expandability.getAmount() * ratio);
                    }
                    element = new Glue(glueSize, 0, 0, this instanceof HorizontalList); // TODO
                }

                line.add(element);
            }

            // Add to the sink.
            output.addElement(makeOutputBox(line));

            thisBreakpoint = nextBreakpoint;
        }
    }

    /**
     * Generate the Box that will be sent to the output.
     */
    protected abstract Box makeOutputBox(List<Element> elements);

    /**
     * Keeps track of possible breakpoints in our paragraph, their penalty, and their effects on the whole paragraph.
     */
    private static class Breakpoint {
        private final int mIndex;
        private final long mPenalty;
        private int mStartIndex;
        private long mBadness;
        private Breakpoint mNextBreakpoint;
        private double mRatio;
        private boolean mRatioIsInfinite;

        private Breakpoint(int index, long penalty) {
            mIndex = index;
            mPenalty = penalty;
            mStartIndex = 0;
            mBadness = 0;
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
         * The badness of the rest of the paragraph starting at this break.
         */
        public long getBadness() {
            return mBadness;
        }

        /**
         * Set the badness of the rest of the paragraph starting at this break.
         */
        public void setBadness(long badness) {
            mBadness = badness;
        }

        /**
         * The next breakpoint in the paragraph assuming we're selected as a breakpoint.
         */
        public Breakpoint getNextBreakpoint() {
            return mNextBreakpoint;
        }

        /**
         * Set the next breakpoint in the paragraph assuming we're selected as a breakpoint.
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

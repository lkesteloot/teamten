package com.teamten.typeset;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Accumulates elements in a horizontal list until a paragraph is finished, at which point a list of
 * elements is generated and added to a vertical list.
 */
public class HorizontalList {
    private final List<Element> mElements = new ArrayList<>();

    public void addElement(Element element) {
        mElements.add(element);
    }

    /**
     * Format the horizontal list and add the elements to the vertical list.
     */
    public void format(VerticalList verticalList, long lineWidth) {
        // Find all the places that we could break a line.
        List<Breakpoint> breakpoints = new ArrayList<>();

        // For convenience put a breakpoint at the very beginning, at the first element.
        breakpoints.add(new Breakpoint(0, 0));

        for (int i = 0; i < mElements.size(); i++) {
            Element element = mElements.get(i);
            Element previousElement = i == 0 ? null : mElements.get(i - 1);

            if (element instanceof Penalty) {
                Penalty penalty = (Penalty) element;
                if (penalty.getPenalty() != Penalty.INFINITY) {
                    breakpoints.add(new Breakpoint(i, penalty.getPenalty()));
                }
            } else if (element instanceof Glue && previousElement instanceof Box) {
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

            // See where the line would start by skipping discardable elements.
            while (index < nextIndex && mElements.get(index) instanceof DiscardableElement) {
                index++;
            }
            breakpoint.setStartIndex(index);
        }

        // Work backwards through the breakpoints. Use dynamic programming to cache the value of what we've
        // computed so far.
        for (int thisBreak = breakpoints.size() - 1; thisBreak >= 0; thisBreak--) {
            Breakpoint thisBreakpoint = breakpoints.get(thisBreak);
            int thisIndex = thisBreakpoint.getStartIndex();

            // So now we're pretending that our paragraph starts here. We're looking for the best next breakpoint.
            Breakpoint bestNextBreakpoint = null;
            long bestBadness = Long.MAX_VALUE;
            double bestRatio = 0;
            for (int nextBreak = thisBreak + 1; nextBreak < breakpoints.size(); nextBreak++) {
                Breakpoint nextBreakpoint = breakpoints.get(nextBreak);
                int nextIndex = nextBreakpoint.getIndex();

                // The first line of our paragraph will go from thisIndex (inclusive) to nextIndex (exclusive).
                // Find the sum of all the elements in that line. Also compute the total stretch and shrink
                // for the glue in that line.
                long width = 0;
                long glueWidth = 0;
                long stretch = 0;
                long shrink = 0;

                for (int i = thisIndex; i < nextIndex; i++) {
                    Element element = mElements.get(i);

                    width += element.getWidth();

                    if (element instanceof Glue) {
                        Glue glue = (Glue) element;
                        glueWidth += glue.getWidth();
                        stretch += glue.getStretch();
                        shrink += glue.getShrink();
                    }
                }

                // Start with our own penalty, since we're breaking here.
                long badness = thisBreakpoint.getPenalty();

                // Computer difference between width and page width.
                long difference = lineWidth - width;

                // See whether we're short or long.
                double ratio;
                if (difference > 0) {
                    // Our line is short. Compute how much we'd have to stretch.
                    if (stretch > 0) {
                        ratio = difference / (double) stretch;
                    } else {
                        // There's no glue to stretch.
                        ratio = Penalty.INFINITY;
                    }
                } else if (difference < 0) {
                    // Our line is long. Compute how much we'd have to shrink.
                    if (shrink > 0) {
                        // This will be negative.
                        ratio = difference / (double) shrink;
                    } else {
                        // There's no glue to shrink.
                        ratio = Penalty.INFINITY;
                    }
                } else {
                    // Our line is just right, there's no extra badness.
                    ratio = 0;
                }

                // See if the ratio is acceptable.
                if (ratio < -1) {
                    // Can't shrink past maximum shrinkage.
                } else {
                    // Factor the ratio into the badness.
                    badness += 100 * Math.pow(Math.abs(ratio), 3);

                    // Add the badness for the paragraph starting at the next breakpoint.
                    badness += nextBreakpoint.getBadness();

                    if (bestNextBreakpoint == null || badness < bestBadness) {
                        bestNextBreakpoint = nextBreakpoint;
                        bestBadness = badness;
                        bestRatio = ratio;
                    }
                }
            }

            if (bestNextBreakpoint != null) {
                // Store the badness at this breakpoint and link to the next break.
                thisBreakpoint.setBadness(bestBadness);
                thisBreakpoint.setNextBreakpoint(bestNextBreakpoint);
                thisBreakpoint.setRatio(bestRatio);
            }
        }

        // Go through our linked list of breakpoints, generating lines.
        Breakpoint thisBreakpoint = breakpoints.get(0);
        while (true) {
            Breakpoint nextBreakpoint = thisBreakpoint.getNextBreakpoint();
            if (nextBreakpoint == null) {
                break;
            }

            int thisIndex = thisBreakpoint.getStartIndex();
            int nextIndex = nextBreakpoint.getIndex();

            // Make a new list with the glue set to specific widths.
            List<Element> line = new ArrayList<>();
            for (Element element : mElements.subList(thisIndex, nextIndex)) {
                if (element instanceof Glue) {
                    Glue glue = (Glue) element;
                    double ratio = thisBreakpoint.getRatio();

                    long glueSize = glue.getSize();
                    if (ratio >= 0) {
                        glueSize += (long) (glue.getStretch() * ratio);
                    } else {
                        glueSize += (long) (glue.getShrink() * ratio);
                    }
                    element = new Glue(glueSize, 0, 0, true);
                }

                line.add(element);
            }

            HBox hbox = new HBox(line);
            verticalList.addElement(hbox);

            thisBreakpoint = nextBreakpoint;
        }
    }

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

        private Breakpoint(int index, long penalty) {
            mIndex = index;
            mPenalty = penalty;
            mStartIndex = 0;
            mBadness = 0;
            mNextBreakpoint = null;
            mRatio = 0;
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
    }
}

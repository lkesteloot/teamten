package com.teamten.typeset;

import java.util.ArrayList;
import java.util.List;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Accumulates elements in a vertical list until the document is finished, at which point a list of
 * pages (type VBox) is generated.
 */
public class VerticalList extends ElementList {
    /**
     * The depth of the last box that was added.
     */
    private long mPreviousDepth = 0;
    /**
     * Whether we've seen a box before.
     */
    private boolean mSawBox = false;
    private long mBaselineSkip = PT.toSp(11*1.2); // Default for 11pt font.

    @Override
    public void addElement(Element element) {
        // Add glue just before boxes so that the baselines are the right distance apart.
        if (element instanceof Box) {
            // Don't do this on the first box.
            if (mSawBox) {
                long skip = Math.max(0, mBaselineSkip - mPreviousDepth - element.getHeight());
                super.addElement(new Glue(skip, 0, 0, false));
            }

            mPreviousDepth = element.getDepth();
            mSawBox = true;
        }

        super.addElement(element);
    }

    /**
     * Specify the distance between baselines. This is normally scaled by the font size,
     * for example 120% of font size. Set this between paragraphs when the font size changes.
     */
    public void setBaselineSkip(long baselineSkip) {
        mBaselineSkip = baselineSkip;
    }

    @Override
    protected Page makeOutputBox(List<Element> elements, int counter) {
        return new Page(elements, counter);
    }

    @Override
    protected long getElementSize(Element element) {
        return element.getHeight() + element.getDepth();
    }

    /**
     * Like {@link #ejectPage()}, but only if the document is not empty.
     */
    public void newPage() {
        if (!getElements().isEmpty()) {
            ejectPage();
        }
    }

    /**
     * Like {@link #newPage()}, but ensures that the next page is an odd page.
     */
    public void oddPage() {
        // Here we have two infinite glues separated by a neutral penalty. The second penalty is forced, but only
        // exists at the end of even pages. This guarantees that the next page will be odd. The second penalty
        // will either skip or include the first penalty, depending on what's best overall.

        // Note that our double-glue works for us because we don't have any other infinite vertical glue on
        // the page. If we were trying to center the text vertically with infinite glue on top, this would not work.

        if (!getElements().isEmpty()) {
            addElement(new Glue(0, PT.toSp(1), true, 0, false, false));
            addElement(new Penalty(0));
            addElement(new Glue(0, PT.toSp(1), true, 0, false, false));
            addElement(new Penalty(-Penalty.INFINITY, true));
        }
    }

    /**
     * Add infinite vertical glue and force a page break.
     */
    public void ejectPage() {
        // Add a final infinite glue at the bottom.
        addElement(new Glue(0, PT.toSp(1), true, 0, false, false));

        // And a forced page break.
        addElement(new Penalty(-Penalty.INFINITY));
    }
}

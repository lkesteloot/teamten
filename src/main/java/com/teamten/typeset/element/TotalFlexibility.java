package com.teamten.typeset.element;

/**
 * Keeps track of a sum of flexibility, along with whether the infinite amount trumps the finite one.
 */
public class TotalFlexibility {
    private long mFiniteAmount = 0;
    private long mInfiniteAmount = 0;

    public void add(Flexibility expandability) {
        if (expandability.isInfinite()) {
            mInfiniteAmount += expandability.getAmount();
        } else {
            mFiniteAmount += expandability.getAmount();
        }
    }

    /**
     * Whether there was non-zero infinite expandability.
     */
    public boolean isInfinite() {
        return mInfiniteAmount != 0;
    }

    /**
     * The expandability, where any finite amount trumps the finite one.
     */
    public long getAmount() {
        return isInfinite() ? mInfiniteAmount : mFiniteAmount;
    }

    /**
     * The total expressed as an immutable Flexibility object.
     */
    public Flexibility toFlexibility() {
        return new Flexibility(getAmount(), isInfinite());
    }
}

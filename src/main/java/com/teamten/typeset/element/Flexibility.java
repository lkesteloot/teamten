package com.teamten.typeset.element;

import static com.teamten.typeset.SpaceUnit.PT;

/**
 * Represents an amount by which a flexible element can stretch or shrink.
 */
public class Flexibility {
    private final long mAmount;
    private final boolean mIsInfinite;

    public Flexibility(long amount, boolean isInfinite) {
        mAmount = amount;
        mIsInfinite = isInfinite;
    }

    /**
     * Amount by which we can stretch or shrink.
     */
    public long getAmount() {
        return mAmount;
    }

    /**
     * Whether this is an infinite stretch or shrink, meaning that non-infinite ones don't factor.
     */
    public boolean isInfinite() {
        return mIsInfinite;
    }

    public String toString(String prefix) {
        if (mAmount == 0) {
            return "";
        } else {
            return String.format("%s%.1f%s", prefix, PT.fromSp(mAmount), mIsInfinite ? "inf" : "pt");
        }
    }
}


package com.teamten.typeset;

import java.io.IOException;
import java.io.Reader;

/**
 * Represents various units to measure 1-dimensional space.
 */
public enum SpaceUnit {
    /**
     * Point. This is the base PDF unit. Note that we don't use the real printer's
     * point, which is 1/72.27 inch. Ours is equivalent to TeX's "big point (bp)".
     */
    PT(65535L),
    /**
     * Pica. One pica = 12 points.
     */
    PC(65535L*12),
    /**
     * Inch. 72 points in an inch, by PDF definition.
     */
    IN(65535L*72),
    /**
     * Centimeter. 2.54 cm in an inch.
     */
    CM(65535L*7200/254),
    /**
     * Millimeter. 25.4 mm in an inch.
     */
    MM(65535L*720/254),
    /**
     * Scaled point, about 5 nm, or 1/100 the wavelength of visible light. We
     * use this for all our internal calculations. A long lets us represent
     * distances up to 49 gigameters.
     */
    SP(1);

    /**
     * The number of SP in this unit.
     */
    private final long mSp;

    SpaceUnit(long sp) {
        mSp = sp;
    }

    /**
     * Convert from this unit to SP.
     */
    public long toSp(double distance) {
        return (long) (distance*mSp + 0.5);
    }

    /**
     * Convert from SP to this unit.
     */
    public double fromSp(double sp) {
        return sp/mSp;
    }

    /**
     * Parse a distance, such as "2in", "3.5 in", or "-2 mm". The number must be
     * parsable as a (possibly signed) double. The unit must be one of the ones
     * from this class, in upper or lower case, preceded by optional whitespace.
     * Only abbreviations are permitted (e.g., "inch" is left after "in" and
     * "centimeter" is rejected). The reader is left immediately after the
     * unit.
     *
     * @return the distance in scaled points.
     * @throws IOException from the Reader.
     * @throws NumberFormatException if the distance cannot be parsed.
     */
    public static long parseDistance(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();

        // Read the double.
        int ch;
        while ((ch = r.read()) != -1) {
            // If it's part of a double, add it to our builder. Luckily none of
            // our units start with an "e".
            if (ch == '-' || Character.isDigit((char) ch) || ch == 'e' || ch == 'E' || ch == '.') {
                sb.append((char) ch);
            }
        }

        // See if we reached the end of the file.
        if (ch == -1) {
            throw new NumberFormatException("missing unit");
        }

        // Parse the value. Might throw a NumberFormatException.
        double value = Double.parseDouble(sb.toString());

        // Skip whitespace.
        while (Character.isWhitespace((char) ch) && (ch = r.read()) != -1) {
            // Nothing.
        }

        // See if we reached the end of the file.
        if (ch == -1) {
            throw new NumberFormatException("missing unit");
        }

        // Read the second character of the unit.
        int ch2 = r.read();

        // See if we reached the end of the file.
        if (ch2 == -1) {
            throw new NumberFormatException("missing unit");
        }

        String unitString = "" + (char) ch + (char) ch2;

        SpaceUnit unit;
        try {
            // Parse unit.
            unit = SpaceUnit.valueOf(unitString.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Invalid unit.
            throw new NumberFormatException("unknown unit " + unitString);
        }

        return unit.toSp(value);
    }
}

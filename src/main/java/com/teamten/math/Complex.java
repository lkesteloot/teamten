// Copyright 2011 Lawrence Kesteloot

package com.teamten.math;

/**
 * An immutable object representing a complex number and its operations.
 */
public class Complex {
    public static final Complex UNITY = new Complex(1, 0);
    private final double mRe;
    private final double mIm;

    public Complex(double re, double im) {
        mRe = re;
        mIm = im;
    }

    /**
     * Creates a new complex number from the phasor (modulus and argument),
     * where: modulus*e^(argument*i) = modulus*(cos(argument) + i*sin(argument))
     */
    public static Complex fromPhasor(double modulus, double argument) {
        if (modulus < 0) {
            throw new IllegalArgumentException(
                    "The modulus cannot be negative (" + modulus + ")");
        }

        return new Complex(modulus * Math.cos(argument),
                modulus * Math.sin(argument));
    }

    /**
     * Returns the real part of this complex number.
     */
    public double re() {
        return mRe;
    }

    /**
     * Returns the imaginary part of this complex number.
     */
    public double im() {
        return mIm;
    }

    /**
     * Returns the modulus of this complex number, i.e., the distance from
     * the number to the complex origin.
     */
    public double modulus() {
        return Math.hypot(mRe, mIm);
    }

    /**
     * Returns the argument of this complex number, i.e., the counterclockwise
     * angle around the complex origin starting at the positive real axis.
     * Always returns a value between -pi and pi.
     */
    public double argument() {
        return Math.atan2(mIm, mRe);
    }

    /**
     * Returns the sum of this and the other complex number.
     */
    public Complex add(Complex other) {
        return new Complex(mRe + other.mRe, mIm + other.mIm);
    }

    /**
     * Returns the difference of this and the other complex number,
     * in other words, this minus other.
     */
    public Complex subtract(Complex other) {
        return new Complex(mRe - other.mRe, mIm - other.mIm);
    }

    /**
     * Returns the negation of this number.
     */
    public Complex negate() {
        return new Complex(-mRe, -mIm);
    }

    /**
     * Returns the conjugate of this number (only imaginary part negated).
     */
    public Complex conjugate() {
        return new Complex(mRe, -mIm);
    }

    /**
     * Returns the reciprocal of this number.
     */
    public Complex reciprocal() {
        return fromPhasor(1 / modulus(), -argument());
    }

    /**
     * Returns the product of this number and the other.
     */
    public Complex multiply(Complex other) {
        return new Complex(mRe*other.mRe - mIm*other.mIm,
                mRe*other.mIm + mIm*other.mRe);
    }

    /**
     * Returns the product of this number and a scalar.
     */
    public Complex multiply(double other) {
        return new Complex(mRe*other, mIm*other);
    }

    /**
     * Returns the quotient of this number and the other, in other
     * words, this divided by other.
     */
    public Complex divide(Complex other) {
        return multiply(other.reciprocal());
    }

    /**
     * Returns the quotient of this number and a scalar.
     */
    public Complex divide(double other) {
        return new Complex(mRe/other, mIm/other);
    }

    /**
     * Returns e raised to this complex number.
     */
    public Complex exp() {
        // e^(re + im*i)
        //     = e^re * e^im*i

        return fromPhasor(Math.exp(mRe), mIm);
    }

    /**
     * Returns the log of this complex number.
     */
    public Complex log() {
        return new Complex(Math.log(modulus()), argument());
    }

    /**
     * Returns the principal Nth root of this number.
     */
    public Complex root(int N) {
        return pow(1.0 / N);
    }

    /**
     * Returns this number to the Nth power.
     */
    public Complex pow(double N) {
        // x^y = e^(log (x^y))
        //     = e^(y*log x)
        return log().multiply(N).exp();
    }
}

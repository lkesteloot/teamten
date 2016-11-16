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

// Copyright 2011 Lawrence Kesteloot

package com.teamten.math;

import java.util.Arrays;

/**
 * Immutable matrix class. Represents each element as a double. All
 * transformation matrices assume column vectors. In other words,
 * a transformation matrix M will transform a column vector v by
 * pre-multiplying it: M*v.
 */
public class Matrix {
    // Easy-access constants for 4x4 matrices. These numbers are [row,column].
    private static final int A11 = 0;
    private static final int A12 = 1;
    private static final int A13 = 2;
    private static final int A14 = 3;
    private static final int A21 = 4;
    private static final int A22 = 5;
    private static final int A23 = 6;
    private static final int A24 = 7;
    private static final int A31 = 8;
    private static final int A32 = 9;
    private static final int A33 = 10;
    private static final int A34 = 11;
    private static final int A41 = 12;
    private static final int A42 = 13;
    private static final int A43 = 14;
    private static final int A44 = 15;
    /**
     * Row-major: mValues[row*mColumnCount + column]
     */
    private final double[] mValues;
    private final int mRowCount;
    private final int mColumnCount;

    /**
     * Private constructor. Keeps a reference to the passed-in array.
     */
    private Matrix(double[] values, int rowCount, int columnCount) {
        if (rowCount * columnCount != values.length) {
            throw new IllegalArgumentException("Size "
                    + getSizeString(rowCount, columnCount)
                    + " doesn't equal number of elements in vector ("
                    + values.length + ")");
        }

        mValues = values;
        mRowCount = rowCount;
        mColumnCount = columnCount;
    }

    /**
     * Public factory that copies its input array.
     */
    public static Matrix make(double[] values, int rowCount, int columnCount) {
        double[] copiedValues = Arrays.copyOf(values, values.length);

        return new Matrix(copiedValues, rowCount, columnCount);
    }

    /**
     * Utility function to create an array of doubles with the
     * diagonals set to 1.
     */
    private static double[] makeUnitArray(int size) {
        double[] values = new double[size*size];

        for (int i = 0; i < size; i++) {
            values[i*size + i] = 1;
        }

        return values;
    }

    /**
     * Make a unit sizexsize matrix.
     */
    public static Matrix makeUnit(int size) {
        return new Matrix(makeUnitArray(size), size, size);
    }

    /**
     * Make a 4x4 matrix with the three vectors as the basis vectors:
     * x is the first row, y the second row, and z the third row. The
     * bottom-right element is 1. When a vector is transformed by
     * this matrix, the input vector will be considered to be in world
     * space, and the transformed vector will be in the basis space.
     */
    public static Matrix makeWithBasis(Vector x, Vector y, Vector z) {
        double[] values = makeUnitArray(4);

        for (int i = 0; i < 3; i++) {
            values[i] = x.get(i);
            values[4 + i] = y.get(i);
            values[8 + i] = z.get(i);
        }

        return new Matrix(values, 4, 4);
    }

    /**
     * Make a 4x4 translation matrix with the given translation.
     */
    public static Matrix makeTranslation(Vector t) {
        double[] values = makeUnitArray(4);

        for (int i = 0; i < 3; i++) {
            values[4*i + 3] = t.get(i);
        }

        return new Matrix(values, 4, 4);
    }

    /**
     * Make a 4x4 matrix with the specified numbers in row-major order.
     */
    public static Matrix make4x4(double... values) {
        if (values.length != 16) {
            throw new IllegalArgumentException("make4x4 must take 16 parameters");
        }

        return new Matrix(values, 4, 4);
    }

    /**
     * Returns the element at (row,column) (zero-based) without checking
     * bounds.
     */
    public double get(int row, int column) {
        return mValues[row*mColumnCount + column];
    }

    /**
     * Return the number of rows.
     */
    public int getRowCount() {
        return mRowCount;
    }

    /**
     * Return the number of columns.
     */
    public int getColumnCount() {
        return mColumnCount;
    }

    /**
     * Return the sum of this and other.
     */
    public Matrix add(Matrix other) {
        assertSameSize(other);

        double[] sum = new double[mValues.length];

        for (int i = 0; i < mValues.length; i++) {
            sum[i] = mValues[i] + other.mValues[i];
        }

        return new Matrix(sum, mRowCount, mColumnCount);
    }

    /**
     * Return the difference of this and other (this minus other).
     */
    public Matrix subtract(Matrix other) {
        assertSameSize(other);

        double[] difference = new double[mValues.length];

        for (int i = 0; i < mValues.length; i++) {
            difference[i] = mValues[i] - other.mValues[i];
        }

        return new Matrix(difference, mRowCount, mColumnCount);
    }

    /**
     * Return the product between this and a scalar.
     */
    public Matrix multiply(double constant) {
        double[] product = new double[mValues.length];

        for (int i = 0; i < mValues.length; i++) {
            product[i] = mValues[i] * constant;
        }

        return new Matrix(product, mRowCount, mColumnCount);
    }

    /**
     * Return the product of the two matrixes (this times other).
     */
    public Matrix multiply(Matrix other) {
        if (mColumnCount != other.mRowCount) {
            throw new IllegalArgumentException(
                    "Matrices do not have compatible size for multiplication: "
                    + getSizeString() + " vs. " + other.getSizeString());
        }

        int newRowCount = mRowCount;
        int newColumnCount = other.mColumnCount;
        int innerCount = mColumnCount;

        double[] product = new double[newRowCount*newColumnCount];

        int i = 0;
        for (int row = 0; row < newRowCount; row++) {
            for (int column = 0; column < newColumnCount; column++) {
                double sum = 0;

                for (int k = 0; k < innerCount; k++) {
                    sum += mValues[row*mColumnCount + k]
                        * other.mValues[k*other.mColumnCount + column];
                }

                product[i] = sum;
                i++;
            }
        }

        return new Matrix(product, newRowCount, newColumnCount);
    }

    /**
     * Transforms the vector by the matrix. The input vector is pre-multiplied
     * by the matrix. The input and output vectors are column vectors and have
     * the same number of dimensions. The matrix must be square. It may have
     * the same size as the vectors, or be one larger, in which case a "1" is
     * added to the input vector and removed from the output vector.
     */
    public Vector transform(Vector vector) {
        if (mRowCount != mColumnCount) {
            throw new IllegalArgumentException(
                    "Matrix must be square for transform ("
                    + getSizeString() + ")");
        }

        int vectorSize = vector.getSize();
        if (vectorSize == mRowCount) {
            double[] newValues = new double[vectorSize];

            for (int row = 0; row < vectorSize; row++) {
                double newValue = 0.0;

                for (int column = 0; column < mColumnCount; column++) {
                    newValue +=
                        vector.get(column)*mValues[row*mColumnCount + column];
                }

                newValues[row] = newValue;
            }

            return new Vector(newValues);
        } else if (vectorSize == mRowCount - 1) {
            double[] newValues = new double[vectorSize];

            for (int row = 0; row < vectorSize; row++) {
                double newValue = 0.0;

                for (int column = 0; column < vectorSize; column++) {
                    newValue +=
                        vector.get(column)*mValues[row*mColumnCount + column];
                }
                // Assume "1" as last element.
                newValue += mValues[row*mColumnCount + mColumnCount - 1];

                newValues[row] = newValue;
            }

            return new Vector(newValues);
        } else {
            throw new IllegalArgumentException("Matrix (" + getSizeString()
                    + ") is wrong size of transform of vector ("
                    + vectorSize + ")");
        }
    }

    /**
     * Transforms a vector (rather than a point). Only the direction is
     * transformed.  Only works on 4x4 matrices and 3-size vectors.
     */
    public Vector transformVector(Vector vector) {
        if (mRowCount != 4 || mColumnCount != 4 || vector.getSize() != 3) {
            throw new IllegalArgumentException("Wrong matrix size ("
                    + getSizeString()
                    + ") or vector size (" + vector.getSize()
                    + ") for vector transform");
        }

        // Crappy way to do it. XXX I think we can just transform by the
        // inverse transform of this matrix, and it might be more correct.
        Vector p1 = transform(Vector.make(0, 0, 0));
        Vector p2 = transform(vector);

        return p2.subtract(p1);
    }

    /**
     * Throws an IllegalArgumentException if the other matrix isn't the same
     * size as this one.
     */
    private void assertSameSize(Matrix other) {
        if (mRowCount != other.mRowCount
                || mColumnCount != other.mColumnCount) {

            throw new IllegalArgumentException(
                    "Matrices must be the same size ("
                    + getSizeString() + " vs. " + other.getSizeString() + ")");
        }
    }

    /**
     * Returns a string like "|1,2,3|\n|4,5,6|\n|7,8,9|" with no newline at the
     * end.
     */
    @Override // Object
    public String toString() {
        return toString("");
    }

    /**
     * Returns a string like "|1,2,3|\n|4,5,6|\n|7,8,9|" with "indent" at the
     * front of each line and no newline at the end.
     */
    public String toString(String indent) {
        StringBuilder builder = new StringBuilder();

        int i = 0;
        for (int row = 0; row < mRowCount; row++) {
            if (row > 0) {
                builder.append('\n');
            }

            builder.append(indent);
            builder.append('|');
            for (int column = 0; column < mColumnCount; column++) {
                if (column > 0) {
                    builder.append(',');
                }
                builder.append(mValues[i]);
                i++;
            }
            builder.append('|');
        }

        return builder.toString();
    }

    /**
     * Return a string of the form "RxC" where R and C are the number of rows
     * and columns in this matrix.
     */
    private String getSizeString() {
        return getSizeString(mRowCount, mColumnCount);
    }

    /**
     * Return a string of the form "RxC" where R and C are the number of rows
     * and columns specified.
     */
    private static String getSizeString(int rowCount, int columnCount) {
        return rowCount + "x" + columnCount;
    }

    /**
     * Returns the inverse of the matrix. Only works on 4x4 matrices.
     */
    public Matrix getInverse() {
        if (getRowCount() != 4 || getColumnCount() != 4) {
            throw new IllegalStateException("can only invert 4x4 matrices");
        }

        double det = getDeterminant();
        if (Math.abs(det) < 1.0e-7) {
            // warn("matrix may be singular(det=%g)\n", det);
        }

        det = 1.0/det;
        Matrix adjoint = getAdjoint();

        // Scale adjoint matrix to get inverse.
        double[] m = adjoint.mValues;
        return Matrix.make4x4(
                m[A11]*det, m[A12]*det, m[A13]*det, m[A14]*det,
                m[A21]*det, m[A22]*det, m[A23]*det, m[A24]*det,
                m[A31]*det, m[A32]*det, m[A33]*det, m[A34]*det,
                m[A41]*det, m[A42]*det, m[A43]*det, m[A44]*det);
    }

    /**
     * Returns the determinant of a 2x2 matrix. Doesn't matter which way (row or column)
     * the numbers are specified.
     */
    private static double det2x2(double a1, double a2, double b1, double b2) {
        return a1*b2 - a2*b1;
    }

    /**
     * Returns the determinant of a 3x3 matrix. Doesn't matter which way (row or column)
     * the numbers are specified.
     */
    private static double det3x3(double a1, double a2, double a3,
            double b1, double b2, double b3,
            double c1, double c2, double c3)  {

        return a1*det2x2(b2, b3, c2, c3) -
            b1*det2x2(a2, a3, c2, c3) +
            c1*det2x2(a2, a3, b2, b3);
    }

    /**
     * Returns the determinant of the matrix. Must be a 4x4 matrix.
     */
    public double getDeterminant() {
        if (getRowCount() != 4 || getColumnCount() != 4) {
            throw new IllegalStateException("can only get determinant of 4x4 matrices");
        }

        double[] m = mValues;

        return
            m[A11]*det3x3(m[A22], m[A23], m[A24], m[A32], m[A33], m[A34], m[A42], m[A43], m[A44]) -
            m[A21]*det3x3(m[A12], m[A13], m[A14], m[A32], m[A33], m[A34], m[A42], m[A43], m[A44]) +
            m[A31]*det3x3(m[A12], m[A13], m[A14], m[A22], m[A23], m[A24], m[A42], m[A43], m[A44]) -
            m[A41]*det3x3(m[A12], m[A13], m[A14], m[A22], m[A23], m[A24], m[A32], m[A33], m[A34]);
    }

    /**
     * Returns the adjoint matrix.
     */
    private Matrix getAdjoint() {
        double[] m = mValues;
        double a1 = m[A11];
        double a2 = m[A12];
        double a3 = m[A13];
        double a4 = m[A14];
        double b1 = m[A21];
        double b2 = m[A22];
        double b3 = m[A23];
        double b4 = m[A24];
        double c1 = m[A31];
        double c2 = m[A32];
        double c3 = m[A33];
        double c4 = m[A34];
        double d1 = m[A41];
        double d2 = m[A42];
        double d3 = m[A43];
        double d4 = m[A44];

        // Row and col labeling reversed since we transpose rows and cols.
        return Matrix.make4x4(
                det3x3(b2, b3, b4, c2, c3, c4, d2, d3, d4),
                -det3x3(a2, a3, a4, c2, c3, c4, d2, d3, d4),
                det3x3(a2, a3, a4, b2, b3, b4, d2, d3, d4),
                -det3x3(a2, a3, a4, b2, b3, b4, c2, c3, c4),

                -det3x3(b1, b3, b4, c1, c3, c4, d1, d3, d4),
                det3x3(a1, a3, a4, c1, c3, c4, d1, d3, d4),
                -det3x3(a1, a3, a4, b1, b3, b4, d1, d3, d4),
                det3x3(a1, a3, a4, b1, b3, b4, c1, c3, c4),

                det3x3(b1, b2, b4, c1, c2, c4, d1, d2, d4),
                -det3x3(a1, a2, a4, c1, c2, c4, d1, d2, d4),
                det3x3(a1, a2, a4, b1, b2, b4, d1, d2, d4),
                -det3x3(a1, a2, a4, b1, b2, b4, c1, c2, c4),

                -det3x3(b1, b2, b3, c1, c2, c3, d1, d2, d3),
                det3x3(a1, a2, a3, c1, c2, c3, d1, d2, d3),
                -det3x3(a1, a2, a3, b1, b2, b3, d1, d2, d3),
                det3x3(a1, a2, a3, b1, b2, b3, c1, c2, c3)
                );
    }

}

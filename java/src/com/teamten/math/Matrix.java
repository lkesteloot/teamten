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
     * bottom-right element is 1.
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
}

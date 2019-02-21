package com.github.rloic.inference;

import com.github.rloic.collections.ArrayExtensions;
import com.github.rloic.collections.UnionFind;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.Objects;

public class DenseMatrix implements InferenceMatrix {

    private static final byte EMPTY = 0;
    private static final byte UNKNOWN = 1;
    private static final byte FIXED_TO_FALSE = 2;
    private static final byte FIXED_TO_TRUE = 3;

    private static final int NO_PIVOT = -1;

    private final byte[][] data;
    private final int rows;
    private final int cols;
    private final boolean[] isBase;
    private final int[] pivotOf;
    private final UnionFind links;

    public DenseMatrix(int[][] equations, int nbVariables) {
        this.rows = equations.length;
        this.cols = nbVariables;
        this.data = new byte[rows][cols];
        this.isBase = new boolean[nbVariables];
        this.pivotOf = new int[nbVariables];
        Arrays.fill(pivotOf, NO_PIVOT);
        this.links = new UnionFind(nbVariables);
        for (int x = 0; x < rows; x++) {
            int[] equation = equations[x];
            for (int y : equation) {
                data[x][y] = UNKNOWN;
            }
        }
        gauss();
    }

    public DenseMatrix(DenseMatrix matrix) {
        this.rows = matrix.rows;
        this.cols = matrix.cols;
        this.data = ArrayExtensions.deepCopy(matrix.data);
        this.isBase = ArrayExtensions.deepCopy(matrix.isBase);
        this.pivotOf = ArrayExtensions.deepCopy(matrix.pivotOf);
        this.links = new UnionFind(matrix.links);
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public int cols() {
        return cols;
    }

    @Override
    public boolean isBase(int variable) {
        return isBase[variable];
    }

    @Override
    public int pivotOf(int variable) {
        return pivotOf[variable];
    }

    @Override
    public void swapBase(int oldBaseVariable, int newBaseVariable) {
        pivotOf[newBaseVariable] = pivotOf[oldBaseVariable];
        isBase[newBaseVariable] = true;
        isBase[oldBaseVariable] = false;
        pivotOf[oldBaseVariable] = -1;
    }

    @Override
    public void xor(int rowA, int rowB) {
        data[rowA] = xor(data[rowA], data[rowB]);
    }

    @Override
    public void fix(int variable, boolean value) {
        for (int x = 0; x < rows; x++) {
            if (data[x][variable] == UNKNOWN) {
                if (value) {
                    data[x][variable] = FIXED_TO_TRUE;
                } else {
                    data[x][variable] = FIXED_TO_FALSE;
                }
            }
        }
    }

    @Override
    public void unfix(int variable) {
        for (int x = 0; x < rows; x++) {
            if (data[x][variable] == FIXED_TO_TRUE || data[x][variable] == FIXED_TO_FALSE) {
                data[x][variable] = UNKNOWN;
            }
        }
    }

    @Override
    public void link(int variableA, int variableB) {
        links.union(variableA, variableB);
    }

    @Override
    public void unlink(int variable) {
        links.cut(variable);
    }

    @Override
    public void removeFromBase(int variable) {
        isBase[variable] = false;
        pivotOf[variable] = NO_PIVOT;
    }

    @Override
    public void appendToBase(int variable, int pivot) {
        isBase[variable] = true;
        pivotOf[variable] = pivot;
    }

    @Override
    public boolean isUnknown(int row, int col) {
        return data[row][col] == UNKNOWN;
    }

    @Override
    public IntList rowsWhereUnknown(int variable) {
        IntList result = new IntArrayList();
        for (int x = 0; x < rows; x++) {
            if (isUnknown(x, variable)) {
                result.add(x);
            }
        }
        return result;
    }

    @Override
    public Inferences infer() {
        Inferences inferences = new Inferences();
        for (byte[] row : data) {
            boolean evenOnes = true;
            int firstIndexOfUnknown = -2;
            int lastIndexOfUnknown = -1;
            int nbUnknowns = 0;
            for (int j = 0; j < row.length; j++) {
                if (row[j] == FIXED_TO_TRUE) {
                    evenOnes = !evenOnes;
                } else if (row[j] == UNKNOWN) {
                    if (firstIndexOfUnknown == -2) {
                        firstIndexOfUnknown = j;
                    }
                    nbUnknowns += 1;
                    lastIndexOfUnknown = j;
                }
            }
            if (lastIndexOfUnknown == firstIndexOfUnknown) {
                if (isBase[firstIndexOfUnknown]) {
                    int pivot = pivotOf[firstIndexOfUnknown];
                    int newBaseVariable = 0;
                    IntList xorBase = new IntArrayList();
                    while (newBaseVariable < cols && (isBase[newBaseVariable] || data[pivot][newBaseVariable] != UNKNOWN)) {
                        if (isBase[newBaseVariable] && newBaseVariable > firstIndexOfUnknown) {
                            xorBase.add(pivotOf[newBaseVariable]);
                        }
                        newBaseVariable += 1;
                    }

                    if (newBaseVariable == cols) {
                        inferences.add(new AffectationWithBaseRemoval(firstIndexOfUnknown, !evenOnes, pivot));
                    } else {
                        IntList xors = rowsWhereUnknown(newBaseVariable);
                        xors.rem(pivot);
                        inferences.add(new AffectationWithBaseChange(firstIndexOfUnknown, !evenOnes, newBaseVariable, pivot, xors, xorBase));
                    }
                } else {
                    inferences.add(new Affectation(firstIndexOfUnknown, !evenOnes));
                }
            } else if (nbUnknowns == 2  && evenOnes && !links.sameSet(firstIndexOfUnknown, lastIndexOfUnknown)) {
                inferences.add(new Equality(firstIndexOfUnknown, lastIndexOfUnknown));
            }
        }
        return inferences;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("bases: ")
                .append(Arrays.toString(isBase))
                .append("\npivots: ")
                .append(Arrays.toString(pivotOf))
                .append("\n");
        for (int y = 0; y < cols; y++) {
            if (isBase[y]) {
                stringBuilder.append("  v ");
            } else {
                stringBuilder.append("    ");
            }
        }
        stringBuilder.append("\n");
        for(int x = 0; x < rows; x++) {
            byte[] line = data[x];
            for (int col = 0; col < cols; col++) {
                byte b = line[col];
                if (b == EMPTY) {
                    stringBuilder.append("  _ ");
                } else if (b == UNKNOWN) {
                    if (links.find(col) != col) {
                        int link = links.find(col);
                        if (link < 10) {
                            stringBuilder.append(" $");
                        } else {
                            stringBuilder.append("$");
                        }
                        stringBuilder.append(link)
                                .append(" ");
                    } else {
                        if (isBase[col] && pivotOf[col] == x) {
                            stringBuilder.append(" (x)");
                        } else {
                            stringBuilder.append("  x ");
                        }

                    }
                } else if (b == FIXED_TO_TRUE) {
                    stringBuilder.append("  T ");
                } else if (b == FIXED_TO_FALSE) {
                    stringBuilder.append("  F ");
                }
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    private void gauss() {
        boolean[] isPivot = new boolean[rows];
        boolean[] hasAOne = new boolean[rows];
        boolean[] hasConflict = new boolean[rows];

        for (int column = 0; column < cols(); column++) {
            for (int row = 0; row < rows(); row++) {
                if (isUnknown(row, column)) {
                    if (!isPivot[row] && !hasAOne[row] && !isBase(column)) {
                        pivotOf[column] = row;
                        isBase[column] = true;
                        isPivot[row] = true;
                    } else {
                        hasConflict[row] = true;
                    }
                    hasAOne[row] = true;
                } else {
                    hasConflict[row] = false;
                }
            }

            if (isBase(column)) {
                byte[] pivot = data[pivotOf(column)];
                for (int x = 0; x < rows; x++) {
                    if (hasConflict[x]) {
                        data[x] = xor(data[x], pivot);
                        if (!isPivot[x]) {
                            byte[] newRow = data[x];
                            hasAOne[x] = hasAOne(newRow, column);
                        }
                        hasConflict[x] = false;
                    }
                }
            }
        }
    }

    private static byte[] xor(byte[] a, byte[] b) {
        byte[] xor = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            if (a[i] == FIXED_TO_TRUE || a[i] == FIXED_TO_FALSE) {
                xor[i] = a[i];
            } else if (b[i] == FIXED_TO_TRUE || b[i] == FIXED_TO_FALSE) {
                if (a[i] == EMPTY) {
                    xor[i] = EMPTY;
                } else { // a[i] = UNKNOWN
                    xor[i] = b[i];
                }
            } else {
                if (a[i] == b[i]) {
                    xor[i] = EMPTY;
                } else {
                    xor[i] = UNKNOWN;
                }
            }
        }
        return xor;
    }

    private static boolean hasAOne(byte[] array, int y) {
        int j = 0;
        while (j <= y && array[j] == EMPTY) {
            j += 1;
        }
        return j != y + 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DenseMatrix)) return false;
        DenseMatrix that = (DenseMatrix) o;
        return rows == that.rows &&
                cols == that.cols &&
                Arrays.deepEquals(data, that.data) &&
                Arrays.equals(isBase, that.isBase) &&
                Arrays.equals(pivotOf, that.pivotOf) &&
                Objects.equals(links, that.links);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rows, cols, links);
        result = 31 * result + Arrays.hashCode(data);
        result = 31 * result + Arrays.hashCode(isBase);
        result = 31 * result + Arrays.hashCode(pivotOf);
        return result;
    }
}

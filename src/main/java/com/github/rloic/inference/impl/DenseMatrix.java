package com.github.rloic.inference.impl;

import com.github.rloic.collections.ArrayExtensions;
import com.github.rloic.collections.UnionFind;
import com.github.rloic.inference.InferenceMatrix;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.exception.ContradictionException;

import java.util.Arrays;
import java.util.BitSet;
import java.util.Objects;

public class DenseMatrix implements InferenceMatrix {

    private static final byte UNKNOWN = 0;
    private static final byte FIXED_TO_FALSE = 1;
    private static final byte FIXED_TO_TRUE = 2;

    private static final int NO_PIVOT = -1;

    private final BitSet[] active;
    private final int rows;
    private final int cols;
    private final boolean[] isBase;
    private final int[] pivotOf;
    private final UnionFind links;
    private final byte[] values;

    public DenseMatrix(int[][] equations, int nbVariables) {
        this.rows = equations.length;
        this.cols = nbVariables;
        this.active = new BitSet[rows];
        this.isBase = new boolean[nbVariables];
        this.pivotOf = new int[nbVariables];
        Arrays.fill(pivotOf, NO_PIVOT);
        this.links = new UnionFind(nbVariables);
        this.values = new byte[nbVariables];
        for (int x = 0; x < rows; x++) {
            int[] equation = equations[x];
            active[x] = new BitSet(nbVariables);
            for (int y : equation) {
                active[x].set(y);
            }
        }
        gauss();
    }

    public DenseMatrix(DenseMatrix matrix) {
        this.rows = matrix.rows;
        this.cols = matrix.cols;
        this.active = new BitSet[rows];
        for (int i = 0; i < rows; i++) {
            active[i] = (BitSet) matrix.active[i].clone();
        }
        this.isBase = ArrayExtensions.deepCopy(matrix.isBase);
        this.pivotOf = ArrayExtensions.deepCopy(matrix.pivotOf);
        this.links = new UnionFind(matrix.links);
        this.values = ArrayExtensions.deepCopy(matrix.values);
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
        pivotOf[oldBaseVariable] = NO_PIVOT;
    }

    @Override
    public void xor(int rowA, int rowB) {
        active[rowA].xor(active[rowB]);
    }

    @Override
    public void fix(int variable, boolean value) throws IllegalStateException {
        check(values[variable] == UNKNOWN, "var_" + variable + " already defined");
        int equivalent = links.find(variable);

        if (value) {
            check(values[equivalent] == UNKNOWN || values[equivalent] == FIXED_TO_TRUE,
                    "broken equality with var_" + links.find(variable) + " when var_" + variable + " is set to " + value);
            for (int row : rowsWhereUnknown(variable)) {
                int col = 0;
                while (col < cols() && !isTrue(row, col) && !isUnknown(row, col)) {
                    col++;
                }
                check(col != cols(),
                        "broken equation inequality with var_" + variable + " set to " + value);
            }
            values[variable] = FIXED_TO_TRUE;
        } else {
            check(values[equivalent] == UNKNOWN || values[equivalent] == FIXED_TO_FALSE,
                    "broken equality with var_" + links.find(variable) + " when var_" + variable + " is set to " + value);
            for (int row : rowsWhereUnknown(variable)) {
                int col = 0;
                int nbTrues = 0;
                while (col < cols && (col == variable || !isUnknown(row, col)) && nbTrues < 2) {
                    if (isTrue(row, col)) {
                        nbTrues += 1;
                    }
                    col++;
                }
                check(col != cols() || nbTrues != 1,
                        "broken equality with var_" + links.find(variable) + " when var_" + variable + " is set to " + value);
            }
            values[variable] = FIXED_TO_FALSE;
        }
    }

    @Override
    public void unfix(int variable) {
        values[variable] = UNKNOWN;
    }

    @Override
    public void link(int variableA, int variableB) {
        links.union(variableA, variableB);
    }

    @Override
    public void unlink(int variable) {
        links.detach(variable);
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
        return active[row].get(col) && values[col] == UNKNOWN;
    }

    @Override
    public boolean isTrue(int row, int col) {
        return active[row].get(col) && values[col] == FIXED_TO_TRUE;
    }

    @Override
    public boolean isFalse(int row, int col) {
        return active[row].get(col) && values[col] == FIXED_TO_FALSE;
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
    public boolean isEquivalent(int varA, int varB) {
        return links.find(varA) == links.find(varB);
    }

    @Override
    public IntList equivalents(int variable) {
        int varSet = links.find(variable);
        IntList equivalents = new IntArrayList();
        for (int i = 0; i < cols(); i++) {
            if (links.find(i) == varSet) {
                equivalents.add(i);
            }
        }
        return equivalents;
    }

    @Override
    public boolean isFixed(int variable) {
        return values[variable] != UNKNOWN;
    }

    @Override
    public boolean isAllFixed() {
        int i = 0;
        while (i < values.length && values[i] != UNKNOWN) {
            i++;
        }
        return i == values.length;
    }

    private void gauss() {
        boolean[] isPivot = new boolean[rows];
        boolean[] hadAOne = new boolean[rows];
        IntList hasConflict = new IntArrayList();
        for (int column = 0; column < cols(); column++) {
            hasConflict.clear();
            for (int row = 0; row < rows(); row++) {
                if (isUnknown(row, column)) {
                    if (!(isPivot[row] || hadAOne[row] || isBase(column))) {
                        pivotOf[column] = row;
                        isBase[column] = true;
                        isPivot[row] = true;
                    } else {
                        hasConflict.add(row);
                    }
                    hadAOne[row] = true;
                }
            }

            if (isBase(column)) {
                BitSet pivot = active[pivotOf(column)];
                for (int row : hasConflict) {
                    active[row].xor(pivot);
                    hadAOne[row] = false;
                }
            }
        }
    }

    private String columnName(int col) {
        if (col < 10) {
            return "  " + col + " ";
        } else if (col < 100) {
            return " " + col + " ";
        } else {
            return col + " ";
        }
    }

    @Override
    public String varsToString() {
        StringBuilder str = new StringBuilder("[");
        for (int i = 0; i < values.length; i++) {
            str.append("BV_" + (i + 1)).append(" = ");
            switch (values[i]) {
                case UNKNOWN:
                    str.append("[0,1]");
                    break;
                case FIXED_TO_TRUE:
                    str.append("1");
                    break;
                case FIXED_TO_FALSE:
                    str.append("0");
                    break;
            }
            str.append(", ");
        }
        if (values.length != 0) {
            str.setLength(str.length() - 2);
        }
        str.append("]");
        return str.toString();
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int y = 0; y < cols; y++) {
            stringBuilder.append(columnName(y));
        }
        stringBuilder.append("\n");
        for (int row = 0; row < rows; row++) {
            for (int col = 0; col < cols; col++) {
                if (isBase[col] && pivotOf[col] == row) {
                    stringBuilder.append(" (");
                } else {
                    stringBuilder.append("  ");
                }
                if (isUnknown(row, col)) {
                    stringBuilder.append('x');
                } else if (isTrue(row, col)) {
                    stringBuilder.append('1');
                } else if (isFalse(row, col)) {
                    stringBuilder.append('0');
                } else {
                    stringBuilder.append('_');
                }
                if (isBase[col] && pivotOf[col] == row) {
                    stringBuilder.append(')');
                } else {
                    stringBuilder.append(' ');
                }
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DenseMatrix)) return false;
        DenseMatrix that = (DenseMatrix) o;
        return rows == that.rows &&
                cols == that.cols &&
                Arrays.equals(active, that.active) &&
                Arrays.equals(isBase, that.isBase) &&
                Arrays.equals(pivotOf, that.pivotOf) &&
                Objects.equals(links, that.links) &&
                Arrays.equals(values, that.values);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(rows, cols, links);
        result = 31 * result + Arrays.hashCode(active);
        result = 31 * result + Arrays.hashCode(isBase);
        result = 31 * result + Arrays.hashCode(pivotOf);
        result = 31 * result + Arrays.hashCode(values);
        return result;
    }
}
package com.github.rloic.paper.impl;

import com.github.rloic.paper.XORMatrix;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.function.IntPredicate;

public class NaiveMatrixImpl implements XORMatrix {

    private static final byte UNDEFINED = 0;
    private static final byte TRUE = 1;
    private static final byte FALSE = -1;

    private static final int NO_PIVOT = -1;

    private final int nbRows;
    private final int nbColumns;
    private final int[] nbUnknowns;
    private final int[] nbTrues;
    private final boolean[][] data;
    private final byte[] valueOf;
    private final boolean[] isBase;
    private final int[] pivotOf;

    private final IntList rows;
    private final IntList columns;

    public NaiveMatrixImpl(int[][] equations, int nbVariables) {
        rows = new IntArrayList(equations.length);
        for(int i = 0; i < equations.length; i++) {
            rows.add(i);
        }
        columns = new IntArrayList(nbVariables);
        for(int j = 0; j < nbVariables; j++) {
            columns.add(j);
        }
        nbRows = equations.length;
        nbColumns = nbVariables;
        nbUnknowns = new int[nbRows];
        nbTrues = new int[nbRows];
        data = new boolean[nbRows][];
        for (int i = 0; i < nbRows; i++) {
            data[i] = new boolean[nbVariables];
            for (int j : equations[i]) {
                data[i][j] = true;
                nbUnknowns[i] += 1;
            }
        }
        valueOf = new byte[nbVariables];
        isBase = new boolean[nbVariables];
        pivotOf = new int[nbVariables];
        Arrays.fill(pivotOf, NO_PIVOT);
    }

    @Override
    public int nbRows() {
        return rows.size();
    }

    @Override
    public int nbColumns() {
        return columns.size();
    }

    @Override
    public IntList rows() {
        return rows;
    }

    @Override
    public IntList columns() {
        return columns;
    }

    @Override
    public boolean isUndefined(int row, int col) {
        return data[row][col] && valueOf[col] == UNDEFINED;
    }

    @Override
    public boolean isFalse(int row, int col) {
        return data[row][col] && valueOf[col] == FALSE;
    }

    @Override
    public boolean isTrue(int row, int col) {
        return data[row][col] && valueOf[col] == TRUE;
    }

    @Override
    public boolean isNone(int row, int col) {
        return !data[row][col];
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
    public void removeVar(int col) {
        assert !isBase[col];
        assert pivotOf[col] == NO_PIVOT;
        columns.removeIf((IntPredicate) i -> i == col);
    }

    @Override
    public void removeRow(int row) {
        rows.removeIf((IntPredicate) i -> i == row);
    }

    @Override
    public int nbUnknowns(int row) {
        return nbUnknowns[row];
    }

    @Override
    public int decrementUnknowns(int row) {
        return --nbUnknowns[row];
    }

    @Override
    public int nbTrues(int row) {
        return nbTrues[row];
    }

    @Override
    public boolean xor(int target, int pivot) {
        int nbUnknownsOfTarget = 0;
        int nbTruesOfTarget = 0;
        for (int j = 0; j < nbColumns(); j++) {
            boolean xor = data[target][j] != data[pivot][j];
            if (xor) {
                if (valueOf[j] == TRUE) {
                    nbTruesOfTarget += 1;
                } else if (valueOf[j] == UNDEFINED) {
                    nbUnknownsOfTarget += 1;
                }
            }
            data[target][j] = xor;
        }
        nbUnknowns[target] = nbUnknownsOfTarget;
        nbTrues[target] = nbTruesOfTarget;
        return nbTruesOfTarget != 1 || nbUnknownsOfTarget != 0;
    }

    @Override
    public void setBase(int pivot, int variable) {
        pivotOf[variable] = pivot;
        isBase[variable] = true;
    }

    @Override
    public void removeBase(int variable) {
        pivotOf[variable] = NO_PIVOT;
        isBase[variable] = false;
    }

    @Override
    public void swap(int rowA, int rowB) {
        int nbTruesOfA = nbTrues[rowA];
        int nbUnknownsOfA = nbUnknowns[rowA];
        boolean[] dataOfRowA = data[rowA];
        nbTrues[rowA] = nbTrues[rowB];
        nbUnknowns[rowA] = nbUnknowns[rowB];
        data[rowA] = data[rowB];
        nbTrues[rowB] = nbTruesOfA;
        nbUnknowns[rowB] = nbUnknownsOfA;
        data[rowB] = dataOfRowA;
    }

    @Override
    public void fix(int variable, boolean value) {
        for(int row : rows()) {
            if (isUndefined(row, variable)) {
                nbUnknowns[row] -= 1;
                if (value) {
                    nbTrues[row] += 1;
                }
            }
        }
        valueOf[variable] = value? TRUE : FALSE;
    }

    @Override
    public int firstUndefined(int row) {
        for (int j : columns()) {
            if (isUndefined(row, j)) return j;
        }
        return -1;
    }

    @Override
    public void incrementUnknowns(int pivot) {
        nbUnknowns[pivot] += 1;
    }

    @Override
    public String toString() {
        StringBuilder str =new StringBuilder();
        for(int i = 0; i < nbRows; i++) {
            for(int j = 0; j < nbColumns; j++) {
                boolean isPivot = isBase[j] && pivotOf[j] == i;
                str.append(isPivot? '(' : ' ');

                if (isNone(i, j)) {
                    str.append('_');
                } else if (isTrue(i, j)) {
                    str.append('1');
                } else if (isFalse(i, j)) {
                    str.append('0');
                } else if (isUndefined(i, j)) {
                    str.append('x');
                }

                str.append(isPivot? ')': ' ');
            }
            str.append(" | nbUnknowns: ")
                  .append(nbUnknowns[i])
                  .append(" | nbTrues: ")
                  .append(nbTrues[i]);
            str.append("\n");
        }
        return str.toString();
    }
}

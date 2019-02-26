package com.github.rloic.paper.impl;

import com.github.rloic.paper.XORMatrix;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class NaiveMatrixImpl implements XORMatrix {

    private static final byte UNDEFINED = 0;
    private static final byte TRUE = 1;
    private static final byte FALSE = -1;

    private boolean[][] data;
    private int rows;
    private int cols;
    private int[] nbTrues;
    private int[] nbUnknowns;
    private boolean[] isBase;
    private int[] pivotOf;
    private byte[] values;
    private List<FixAction> stack;

    public NaiveMatrixImpl(boolean[][] data, int rows, int cols) {
        this.data = data;
        this.rows = rows;
        this.cols = cols;
        nbUnknowns = new int[rows];
        isBase = new boolean[cols];
        pivotOf = new int[cols];
        Arrays.fill(pivotOf, -1);
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (data[i][j]) {
                    nbUnknowns[i] += 1;
                }
            }
        }
        nbTrues = new int[rows];
        values = new byte[cols];
        stack = new ArrayList<>();
    }

    @Override
    public int cols() {
        return cols;
    }

    @Override
    public int rows() {
        return rows;
    }

    @Override
    public boolean isUnknown(int row, int variable) {
        return data[row][variable];
    }

    @Override
    public boolean isUndefined(int variable) {
        return values[variable] == UNDEFINED;
    }

    @Override
    public int nbUnknowns(int row) {
        return nbUnknowns[row];
    }

    @Override
    public int nbTrues(int row) {
        return nbTrues[row];
    }

    @Override
    public void xor(int rowA, int rowB) {
        int unknownsOnLine = 0;
        int truesOnLine = 0;
        for (int j = 0; j < cols; j++) {
            boolean xor = data[rowA][j] != data[rowB][j];
            if (xor) {
                if(values[j] == TRUE) {
                    truesOnLine += 1;
                } else if (values[j] == UNDEFINED) {
                    unknownsOnLine += 1;
                }
            }
            data[rowA][j] = xor;
        }
        nbUnknowns[rowA] = unknownsOnLine;
        nbTrues[rowA] = truesOnLine;
    }

    @Override
    public void appendToBase(int pivot, int variable) {
        pivotOf[variable] = pivot;
        isBase[variable] = true;
    }

    @Override
    public void fix(int variable, boolean value) {
        values[variable] = value ? TRUE : FALSE;
        for (int i = 0; i < rows(); i++) {
            if (isUnknown(i, variable)) {
                nbUnknowns[i] -= 1;
                if (value) {
                    nbTrues[i] += 1;
                }
            }
        }
        if (isBase(variable)) {
            int newBaseVar = 0;
            while (newBaseVar < cols && (!isUnknown(pivotOf[variable], newBaseVar) || values[newBaseVar] != UNDEFINED || newBaseVar == variable)) {
                newBaseVar++;
            }
            if (newBaseVar < cols) {
                swapBase(variable, newBaseVar);
                IntList memXors = new IntArrayList();
                for (int i = 0; i < rows(); i++) {
                    if (i != pivotOf[newBaseVar] && isUnknown(i, newBaseVar)) {
                        xor(i, pivotOf[newBaseVar]);
                        memXors.add(i);
                    }
                }
                stack.add(new FixActionWithBaseSwap(variable, value, newBaseVar, memXors));
            } else {
                stack.add(new FixActionWithBaseRemoval(variable, value, pivotOf[variable]));
                removeFromBase(variable);
            }
        } else {
            stack.add(new FixAction(variable, value));
        }
    }

    @Override
    public void rollback() {
        FixAction action = stack.remove(stack.size() - 1);
        if (action instanceof FixActionWithBaseSwap) {
                FixActionWithBaseSwap a = (FixActionWithBaseSwap) action;
                for(int i : a.xors) {
                    xor(i, pivotOf[a.newBaseVar]);
                }
                swapBase(a.newBaseVar, action.variable);
        } else if (action instanceof FixActionWithBaseRemoval) {
                FixActionWithBaseRemoval a = (FixActionWithBaseRemoval) action;
                appendToBase(a.pivot, a.variable);
        }
        for (int i = 0; i < rows(); i++) {
            if (isUnknown(i, action.variable)) {
                nbUnknowns[i] += 1;
                if (action.value) {
                    nbTrues[i] -= 1;
                }
            }
        }
        values[action.variable] = UNDEFINED;
    }

    @Override
    public int pivotOf(int variable) {
        return pivotOf[variable];
    }

    @Override
    public boolean isBase(int variable) {
        return isBase[variable];
    }

    @Override
    public void removeFromBase(int variable) {
        isBase[variable] = false;
        pivotOf[variable] = -1;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int j = 0; j < cols; j++) {
            if (isBase(j)) {
                str.append(" v");
            } else {
                str.append("  ");
            }
        }
        str.append("\n");
        for (int i = 0; i < rows(); i++) {
            for (int j = 0; j < cols(); j++) {
                if (isUnknown(i, j)) {
                    switch (values[j]) {
                        case UNDEFINED:
                            str.append(" x");
                            break;
                        case TRUE:
                            str.append(" 1");
                            break;
                        case FALSE:
                            str.append(" 0");
                    }
                } else {
                    str.append(" _");
                }
            }
            str.append(" | nbUnknowns: ")
                    .append(nbUnknowns[i])
                    .append(" | nbTrues: ")
                    .append(nbTrues[i])
                    .append("\n");
        }
        return str.toString();
    }
}

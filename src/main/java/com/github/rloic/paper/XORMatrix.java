package com.github.rloic.paper;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.util.ESat;

public interface XORMatrix {

    int cols();
    int rows();
    boolean isUnknown(int row, int variable);
    boolean isUndefined(int variable);
    boolean isFixedToTrue(int variable);
    boolean isFixedToFalse(int variable);
    int nbUnknowns(int row);
    int nbTrues(int row);
    int pivotOf(int variable);
    boolean isBase(int variable);

    boolean xor(int rowA, int rowB);
    void fix(int variable, boolean value) throws ContradictionException;
    void rollback();
    void appendToBase(int pivot, int variable);
    void removeFromBase(int variable);

    default void swapBase(int oldBaseVariable, int newBaseVariable) {
        appendToBase(pivotOf(oldBaseVariable), newBaseVariable);
        removeFromBase(oldBaseVariable);
    }

    boolean isFixed(int variable);
    boolean isFull();

    static void normalize(XORMatrix M) {
        boolean[] isPivot = new boolean[M.rows()];
        boolean[] hadAOne = new boolean[M.rows()];
        IntList hasConflict = new IntArrayList();
        for (int column = 0; column < M.cols(); column++) {
            hasConflict.clear();
            for (int row = 0; row < M.rows(); row++) {
                if (M.isUnknown(row, column)) {
                    if (!(isPivot[row] || hadAOne[row] || M.isBase(column))) {
                        M.appendToBase(row, column);
                        isPivot[row] = true;
                    } else {
                        hasConflict.add(row);
                    }
                    hadAOne[row] = true;
                }
            }

            if (M.isBase(column)) {
                int pivot = M.pivotOf(column);
                for (int row : hasConflict) {
                    M.xor(row, pivot);
                    hadAOne[row] = false;
                }
            }
        }
    }

}

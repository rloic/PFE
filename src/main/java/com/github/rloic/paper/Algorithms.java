package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.impl.InferenceEngineImpl;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Algorithms {

    public static void main(String[] args) {

        XORMatrix matrix = new NaiveMatrixImpl(
                new boolean[][]{
                        new boolean[]{false, false, true, false, false, true, true},
                        new boolean[]{false, false, false, true, true, true, false},
                        new boolean[]{false, true, true, false, true, false, false},
                        new boolean[]{true, true, false, true, false, false, false}
                }, 4, 7
        );
        InferenceEngine engine = new InferenceEngineImpl();
        Algorithms.normalize(matrix);

        System.out.println(matrix);
        engine.applyAndInfer(matrix, new Affectation(0, false));
        System.out.println(matrix);
        engine.applyAndInfer(matrix, new Affectation(6, false));
        System.out.println(matrix);
        matrix.rollback();
        System.out.println(matrix);
        matrix.rollback();
        System.out.println(matrix);
    }

    private static void normalize(XORMatrix M) {
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

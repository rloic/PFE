package com.github.rloic.inference.impl;

import com.github.rloic.inference.InferenceEngine;
import com.github.rloic.inference.InferenceMatrix;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.github.rloic.collections.ArrayExtensions.intArrayOf;

class DenseMatrixTest {

    private InferenceMatrix emptyMatrix() {
        return new DenseMatrix(new int[][] {}, 0);
    }

    private final int A = 0;
    private final int B = 1;
    private final int C = 2;
    private final int D = 3;
    private final int E = 4;
    private final int F = 5;
    private final int G = 6;

    /* Toy matrix
         A   B   C   D   E   F   G
         v   v   v   v
         _   _  (x)  _   _   x   x
         _   _   _  (x)  x   x   _
         _  (x)  _   _   x   x   x
        (x)  _   _   _   _   _   x
     */
    private InferenceMatrix toyMatrix() {
        return new DenseMatrix(
                new int[][]{
                        new int[]{2, 5, 6},
                        new int[]{4, 3, 5},
                        new int[]{1, 4, 2},
                        new int[]{1, 3, 0},
                },
                7
        );
    }

    @Test
    void empty_matrix_should_be_fixed() {
        final InferenceMatrix matrix = emptyMatrix();
        Assertions.assertTrue(matrix.isAllFixed());
    }

    @Test
    void matrix_with_unknowns_should_not_be_fixed() {
        final InferenceMatrix matrix = toyMatrix();
        Assertions.assertFalse(matrix.isAllFixed());
    }

    @Test
    void remove_base_should_remove_base_and_pivot() {
        final InferenceMatrix matrix = toyMatrix();

        final int variable = 0;
        Assertions.assertTrue(matrix.isBase(variable));
        Assertions.assertNotEquals(-1, matrix.pivotOf(variable));

        matrix.removeFromBase(variable);
        Assertions.assertFalse(matrix.isBase(variable));
        Assertions.assertEquals(-1, matrix.pivotOf(variable));
    }

    @Test
    void swap_base_should_swap_base_and_pivot() {
        final InferenceMatrix matrix = toyMatrix();

        final int baseVariable = 0;
        Assertions.assertTrue(matrix.isBase(baseVariable));
        Assertions.assertNotEquals(-1, matrix.pivotOf(baseVariable));
        final int pivot = matrix.pivotOf(baseVariable);

        final int nonBaseVariable = 4;
        Assertions.assertFalse(matrix.isBase(nonBaseVariable));
        Assertions.assertEquals(-1, matrix.pivotOf(nonBaseVariable));

        matrix.swapBase(baseVariable, nonBaseVariable);
        Assertions.assertTrue(matrix.isBase(nonBaseVariable));
        Assertions.assertEquals(pivot, matrix.pivotOf(nonBaseVariable));

        Assertions.assertFalse(matrix.isBase(baseVariable));
        Assertions.assertEquals(-1, matrix.pivotOf(baseVariable));
    }

    @Test
    void xor_twice_must_rollback() {
        final InferenceMatrix matrix = toyMatrix();
        final InferenceMatrix copy = toyMatrix();

        matrix.xor(0, 1);
        matrix.xor(0, 1);
        Assertions.assertEquals(copy, matrix);
    }

    @Test
    void link_and_unlink_must_rollback() {
        final InferenceMatrix matrix = toyMatrix();
        final InferenceMatrix copy = toyMatrix();

        matrix.link(0, 2);
        matrix.unlink(0);
        Assertions.assertEquals(copy, matrix);
    }

    @Test
    void fix_and_unfix_must_rollback() {
        final InferenceMatrix matrix = toyMatrix();
        final InferenceMatrix copy = toyMatrix();

        matrix.fix(0, false);
        matrix.unfix(0);
        Assertions.assertEquals(copy, matrix);
    }

    @Test
    void swap_base_twice_must_rollback() {
        final InferenceMatrix matrix = toyMatrix();
        final InferenceMatrix copy = toyMatrix();

        matrix.swapBase(0, 6);
        matrix.swapBase(6, 0);
        Assertions.assertEquals(copy, matrix);
    }

    @Test
    void pivot_and_base_must_be_sync() {
        final InferenceMatrix matrix = toyMatrix();

        for(int variable = 0; variable < matrix.cols(); variable++) {
            Assertions.assertEquals(-1 == matrix.pivotOf(variable), !matrix.isBase(variable));
        }
        matrix.removeFromBase(0);
        matrix.removeFromBase(1);
        for(int variable = 0; variable < matrix.cols(); variable++) {
            Assertions.assertEquals(-1 == matrix.pivotOf(variable), !matrix.isBase(variable));
        }

        matrix.appendToBase(0, 4);
        matrix.appendToBase(1, 3);
        for(int variable = 0; variable < matrix.cols(); variable++) {
            Assertions.assertEquals(-1 == matrix.pivotOf(variable), !matrix.isBase(variable));
        }
    }

    @Test
    void where_unknowns() {
        final InferenceMatrix matrix = toyMatrix();
        IntList rowsWhereVar5IsUnkown = matrix.rowsWhereUnknown(5);
        IntList expectedRowsWhereVar5IsActivated = new IntArrayList(intArrayOf(0, 1, 2));
        Assertions.assertEquals(expectedRowsWhereVar5IsActivated, rowsWhereVar5IsUnkown);

        IntList rowsWhereVar6IsUnknown = matrix.rowsWhereUnknown(6);
        IntList expectedRowsWhereVar6IsUnknown = new IntArrayList(intArrayOf(0, 2, 3));
        Assertions.assertEquals(expectedRowsWhereVar6IsUnknown, rowsWhereVar6IsUnknown);
    }

    @Test()
    void fix_unvalid_values_must_failed() {
        InferenceMatrix matrix = toyMatrix();
        final InferenceEngine engine = new InferenceEngineImpl();
        engine.inferAndUpdate(matrix);
        Affectation A1 =
                engine.createAffectation(matrix, A, true);
        Affectation G0 =
                engine.createAffectation(matrix, G, false);

        /* Toy matrix
          A   B   C   D   E   F   G
          _   _  (x)  _   _   x   x
          _   _   _  (x)  x   x   _
          _  (x)  _   _   x   x   x
          1  _   _   _   _   _    0 <- A = 1 & G = 0 => A == G is broken
        */
        A1.apply(matrix);
        final InferenceMatrix lambdaArg = matrix;
        Assertions.assertThrows(AssertionError.class, () -> G0.apply(lambdaArg));

        matrix = toyMatrix();
        Affectation A0 =
                engine.createAffectation(matrix, A, false);
        Affectation F0 =
                engine.createAffectation(matrix, F, false);
        Affectation C1 = engine.createAffectation(matrix, C, true);

        /* Toy matrix
          A   B   C   D   E   F   G
          _   _  (x)  _   _   0   0    A = 0 => G == 0
          _   _   _  (x)  x   0   _    F = 0 & A = 0 => C == 0
          _  (x)  _   _   x   0   0
          0  _   _   _   _   _    0
        */
        A0.apply(matrix);
        engine.inferAndUpdate(matrix);
        F0.apply(matrix);
        engine.inferAndUpdate(matrix);
        final InferenceMatrix lambda2Arg = matrix;
        Assertions.assertThrows(AssertionError.class, () -> C1.apply(lambda2Arg));
    }

}
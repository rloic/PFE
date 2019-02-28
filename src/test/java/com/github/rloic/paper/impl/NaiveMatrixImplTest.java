package com.github.rloic.paper.impl;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.InferenceEngine;
import com.github.rloic.paper.XORMatrix;
import org.chocosolver.solver.exception.ContradictionException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NaiveMatrixImplTest {

    private XORMatrix emptyMatrix() {
        return new NaiveMatrixImpl(new int[][]{}, 0);
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
    private XORMatrix toyMatrix() {
        return new NaiveMatrixImpl(
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
    void empty_matrix_should_be_full() {
        final XORMatrix matrix = emptyMatrix();
        assertTrue(matrix.isFull());
    }

    @Test
    void matrix_with_unknowns_should_not_be_full() {
        final XORMatrix matrix = toyMatrix();
        assertFalse(matrix.isFull());
    }

    @Test
    void remove_base_should_remove_base_and_pivot() {
        final XORMatrix matrix = toyMatrix();

        final int variable = 0;
        assertTrue(matrix.isBase(variable));
        assertNotEquals(-1, matrix.pivotOf(variable));

        matrix.removeFromBase(variable);
        assertFalse(matrix.isBase(variable));
        assertEquals(-1, matrix.pivotOf(variable));
    }

    @Test
    void swap_base_should_swap_base_and_pivot() {
        final XORMatrix matrix = toyMatrix();

        final int baseVariable = 0;
        assertTrue(matrix.isBase(baseVariable));
        assertNotEquals(-1, matrix.pivotOf(baseVariable));
        final int pivot = matrix.pivotOf(baseVariable);

        final int nonBaseVariable = 4;
        assertFalse(matrix.isBase(nonBaseVariable));
        assertEquals(-1, matrix.pivotOf(nonBaseVariable));

        matrix.swapBase(baseVariable, nonBaseVariable);
        assertTrue(matrix.isBase(nonBaseVariable));
        assertEquals(pivot, matrix.pivotOf(nonBaseVariable));

        assertFalse(matrix.isBase(baseVariable));
        assertEquals(-1, matrix.pivotOf(baseVariable));
    }

    @Test
    void xor_twice_must_rollback() {
        final XORMatrix matrix = toyMatrix();
        final XORMatrix copy = toyMatrix();

        matrix.xor(0, 1);
        matrix.xor(0, 1);
        assertEquals(copy, matrix);
    }

    @Test
    void rollback_after_fix_must_restore_previous_state() throws ContradictionException {
        final XORMatrix matrix = toyMatrix();
        final XORMatrix copy = toyMatrix();

        matrix.fix(0, false);
        matrix.rollback();
        assertEquals(copy, matrix);
    }

    @Test
    void swap_base_twice_must_rollback() {
        final XORMatrix matrix = toyMatrix();
        final XORMatrix copy = toyMatrix();

        matrix.swapBase(0, 6);
        matrix.swapBase(6, 0);
        assertEquals(copy, matrix);
    }

    @Test
    void pivot_and_base_must_be_sync() {
        final XORMatrix matrix = toyMatrix();

        for (int variable = 0; variable < matrix.cols(); variable++) {
            assertEquals(-1 == matrix.pivotOf(variable), !matrix.isBase(variable));
        }
        matrix.removeFromBase(0);
        matrix.removeFromBase(1);
        for (int variable = 0; variable < matrix.cols(); variable++) {
            assertEquals(-1 == matrix.pivotOf(variable), !matrix.isBase(variable));
        }

        matrix.appendToBase(0, 4);
        matrix.appendToBase(1, 3);
        for (int variable = 0; variable < matrix.cols(); variable++) {
            assertEquals(-1 == matrix.pivotOf(variable), !matrix.isBase(variable));
        }
    }

    @Test()
    void fix_unvalid_values_must_failed() throws ContradictionException {
        XORMatrix matrix = toyMatrix();
        final InferenceEngine engine = new InferenceEngineImpl();
        Affectation A1 =
                new Affectation(A, true);
        final Affectation G0 =
                new Affectation(G, false);

        /* Toy matrix
          A   B   C   D   E   F   G
          _   _  (x)  _   _   x   x
          _   _   _  (x)  x   x   _
          _  (x)  _   _   x   x   x
          1  _   _   _   _   _    0 <- A = 1 & G = 0 => A == G is broken
        */

        engine.applyAndInfer(matrix, A1);
        final XORMatrix lambdaArg = matrix;
        assertThrows(ContradictionException.class, () ->
                engine.applyAndInfer(lambdaArg, G0)
        );

        matrix = toyMatrix();
        Affectation A0 =
                new Affectation(A, false);
        Affectation F0 =
                new Affectation(F, false);
        Affectation C1 =
                new Affectation(C, true);

        /* Toy matrix
          A   B   C   D   E   F   G
          _   _  (x)  _   _   0   0    A = 0 => G == 0
          _   _   _  (x)  x   0   _    F = 0 & A = 0 => C == 0
          _  (x)  _   _   x   0   0
          0  _   _   _   _   _    0
        */
        engine.applyAndInfer(matrix, A0);
        engine.applyAndInfer(matrix, G0);
        engine.applyAndInfer(matrix, F0);
        final XORMatrix lambda2Arg = matrix;
        assertThrows(ContradictionException.class, () ->
                engine.applyAndInfer(lambda2Arg, C1)
        );
    }


}
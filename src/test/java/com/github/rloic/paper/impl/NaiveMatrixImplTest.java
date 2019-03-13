package com.github.rloic.paper.impl;

import com.github.rloic.paper.Algorithms;
import com.github.rloic.paper.XORMatrix;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

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
         _   _  (equation)  _   _   equation   equation
         _   _   _  (equation)  equation   equation   _
         _  (equation)  _   _   equation   equation   equation
        (equation)  _   _   _   _   _   equation
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
    void should_do_something() {
        XORMatrix matrix = toyMatrix();
        Algorithms.normalize(matrix, new int[][]{}, new ArrayList<>());
        matrix.fix(C, true);
        System.out.println("");
    }


}
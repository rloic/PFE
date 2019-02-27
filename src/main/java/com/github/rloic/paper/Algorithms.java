package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.impl.InferenceEngineImpl;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Algorithms {
    private static final int A = 0;
    private static final int B = 1;
    private static final int C = 2;
    private static final int D = 3;
    private static final int E = 4;
    private static final int F = 5;
    private static final int G = 6;
    private static final int H = 7;
    private static final int I = 8;
    private static final int J = 9;

    public static void main(String[] args) {

        XORMatrix matrix = new NaiveMatrixImpl(new int[][]{
                new int[]{C, F, G},
                new int[]{E, D, F},
                new int[]{B, E, C},
                new int[]{B, D, A},
        }, 7);
        InferenceEngine engine = new InferenceEngineImpl();
        System.out.println(matrix);
    }

}

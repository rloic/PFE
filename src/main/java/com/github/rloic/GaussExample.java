package com.github.rloic;

import com.github.rloic.inference.Inference;
import com.github.rloic.inference.InferenceEngine;
import com.github.rloic.inference.impl.*;
import com.github.rloic.util.Logger;

import java.util.Scanner;

import static com.github.rloic.util.Logger.TraceLogger.TRACE;

public class GaussExample {

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

    static Scanner reader = new Scanner(System.in);
    private static final InferenceEngine engine = new InferenceEngineImpl();

    public static void main(String[] args) {
        Logger.level(TRACE);

        DenseMatrix m = new DenseMatrix(new int[][]{
                new int[]{C, F, G},
                new int[]{E, D, F},
                new int[]{B, E, C},
                new int[]{B, D, A},
        }, 7);

        DenseMatrix copy = new DenseMatrix(m);

        System.out.println("Initial matrix");
        Inferences step0 = engine.inferAndUpdate(m);
        Affectation cIsTrue = engine.createAffectation(m, 2, true);
        cIsTrue.apply(m);
        System.out.print(cIsTrue + " => ");
        Inferences step1 = engine.inferAndUpdate(m);
        fIsFalse.apply(m);
        System.out.print(fIsFalse + " => ");
        Inferences step2 = engine.inferAndUpdate(m);
        engine.createAffectation(m,4, false).apply(m);
        engine.inferAndUpdate(m);
    }

    private static Inference fIsFalse = new AffectationWithBaseRemoval(5, false, 0);


}

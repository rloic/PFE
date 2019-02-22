package com.github.rloic;

import com.github.rloic.inference.Inference;
import com.github.rloic.inference.InferenceEngine;
import com.github.rloic.inference.impl.*;
import com.github.rloic.util.Logger;

import java.util.Scanner;

import static com.github.rloic.util.Logger.TraceLogger.TRACE;

public class GaussExample {

    static Scanner reader = new Scanner(System.in);
    private static final InferenceEngine engine = new InferenceEngineImpl();

    public static void main(String[] args) {
        Logger.level(TRACE);

        DenseMatrix m = new DenseMatrix(new int[][]{
                new int[]{2, 5, 6},
                new int[]{4, 3, 5},
                new int[]{1, 4, 2},
                new int[]{1, 3, 0},
        }, 7);

        DenseMatrix copy = new DenseMatrix(m);

        System.out.println("Initial matrix");
        Inferences step0 = engine.infer(m);
        Affectation cIsTrue = engine.createAffectation(m, 2, true);
        cIsTrue.apply(m);
        System.out.print(cIsTrue + " => ");
        Inferences step1 = engine.infer(m);
        fIsFalse.apply(m);
        System.out.print(fIsFalse + " => ");
        Inferences step2 = engine.infer(m);
        engine.createAffectation(m,4, false).apply(m);
        engine.infer(m);

    }

    private static Inference fIsFalse = new AffectationWithBaseRemoval(5, false, 0);


}

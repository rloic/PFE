package com.github.rloic.paper.impl;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.Inferences;
import com.github.rloic.paper.InferenceEngine;
import com.github.rloic.paper.XORMatrix;

public class InferenceEngineImpl implements InferenceEngine {

    @Override
    public Inferences applyAndInfer(XORMatrix matrix, Affectation affectation) {
        matrix.fix(affectation.variable, affectation.value);
        Inferences inferences = new Inferences();
        for (int i = 0; i < matrix.rows(); i++) {
            if (matrix.isUnknown(i, affectation.variable)) {
                if (matrix.nbUnknowns(i) == 1 && matrix.nbTrues(i) <= 1) {
                    int j = 0;
                    while (!matrix.isUnknown(i, j) || !matrix.isUndefined(j) || j == affectation.variable) {
                        j += 1;
                    }
                    inferences.add(new Affectation(j, matrix.nbTrues(i) == 1));
                }
            }
        }
        return inferences;
    }
}
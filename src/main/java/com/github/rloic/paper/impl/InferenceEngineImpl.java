package com.github.rloic.paper.impl;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.InferenceEngine;
import com.github.rloic.paper.XORMatrix;
import org.chocosolver.solver.exception.ContradictionException;

import java.util.List;

public class InferenceEngineImpl implements InferenceEngine {

    @Override
    public List<Affectation> applyAndInfer(XORMatrix matrix, Affectation affectation) throws ContradictionException {

        throw new IllegalStateException();
    }
}

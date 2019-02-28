package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import org.chocosolver.solver.exception.ContradictionException;

import java.util.List;

public interface InferenceEngine {

    List<Affectation> applyAndInfer(XORMatrix matrix, Affectation affectation) throws ContradictionException;

}

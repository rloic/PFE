package com.github.rloic.inference;

import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;

public interface Inference {
    void apply(InferenceMatrix matrix) throws IllegalStateException;
    void unapply(InferenceMatrix matrix);
    void constraint(BoolVar[] vars, ICause cause) throws ContradictionException;
}

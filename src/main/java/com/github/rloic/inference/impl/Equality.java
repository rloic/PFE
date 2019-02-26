package com.github.rloic.inference.impl;

import com.github.rloic.inference.Inference;
import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;

public class Equality implements Inference {

    private final int lhs;
    private final int rhs;

    public Equality(int lhs, int rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void apply(InferenceMatrix matrix) throws IllegalStateException {
        Logger.trace("Link var_" + lhs + " with var_" + rhs);
        matrix.link(lhs, rhs);
        Logger.trace("\n" + matrix);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        Logger.trace("Unlink var_" + lhs + " from var_" + rhs);
        matrix.unlink(lhs);
        Logger.trace("\n" + matrix);
    }

    @Override
    public void constraint(BoolVar[] vars, ICause cause) throws ContradictionException {
    }

    @Override
    public String toString() {
        return "(var_" + lhs + "==var_" + rhs + ")";
    }
}

package com.github.rloic.inference.impl;

import com.github.rloic.inference.Inference;
import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;

public class Affectation implements Inference {

    public final int variable;
    public final boolean value;

    public Affectation(int variable, boolean value) {
        this.variable = variable;
        this.value = value;
    }

    @Override
    public void apply(InferenceMatrix matrix) throws IllegalStateException {
        Logger.trace("Fix var_" + variable + " to " + value);
        matrix.fix(variable, value);
        Logger.trace("\n" + matrix);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        Logger.trace("Unfix var_" + variable);
        matrix.unfix(variable);
        Logger.trace("\n" + matrix);
    }

    @Override
    public void constraint(BoolVar[] vars, ICause cause) throws ContradictionException {
        if (value) {
            vars[variable].setToTrue(cause);
        } else {
            vars[variable].setToFalse(cause);
        }
    }

    @Override
    final public String toString() {
        return "(var_" + variable + "<-" + value + ")";
    }
}

package com.github.rloic.inference.impl;

import com.github.rloic.inference.IAffectation;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;

import java.util.Objects;

public abstract class Affectation implements IAffectation {

    public final int variable;
    public final boolean value;

    public Affectation(int variable, boolean value) {
        this.variable = variable;
        this.value = value;
    }

    final void raiseConstradiction() throws ContradictionException {
        throw new ContradictionException();
    }

    @Override
    final public int variable() {
        return variable;
    }

    @Override
    final public boolean value() {
        return value;
    }

    @Override
    public void propagate(BoolVar[] vars, ICause cause) throws ContradictionException {
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

    @Override
    final public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Affectation)) return false;
        Affectation that = (Affectation) o;
        return variable == that.variable &&
              value == that.value;
    }

    @Override
    final public int hashCode() {
        return Objects.hash(variable, value);
    }
}

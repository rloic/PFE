package com.github.rloic.inference.impl;

import com.github.rloic.inference.Inference;
import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.util.Logger;

public class Affectation implements Inference {

    final int variable;
    final boolean value;

    public Affectation(int variable, boolean value) {
        this.variable = variable;
        this.value = value;
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        Logger.debug("Fix var_" + variable + " to " + value);
        matrix.fix(variable, value);
        Logger.trace("\n" + matrix);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        Logger.debug("Unfix var_" + variable);
        matrix.unfix(variable);
        Logger.trace("\n" + matrix);
    }

    @Override
    final public String toString() {
        return "(var_" + variable + "<-" + value + ")";
    }
}

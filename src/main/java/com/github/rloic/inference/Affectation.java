package com.github.rloic.inference;

public class Affectation implements Inference {

    final int variable;
    final boolean value;

    public Affectation(int variable, boolean value) {
        this.variable = variable;
        this.value = value;
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        matrix.fix(variable, value);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        matrix.unfix(variable);
    }

    @Override
    final public String toString() {
        return "(var_" + variable + "<-" + value + ")";
    }
}

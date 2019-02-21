package com.github.rloic.inference;

public class AffectationWithBaseRemoval extends Affectation {

    private final int pivot;

    public AffectationWithBaseRemoval(int variable, boolean value, int pivot) {
        super(variable, value);
        this.pivot = pivot;
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        matrix.fix(variable, value);
        matrix.removeFromBase(variable);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        matrix.appendToBase(variable, pivot);
        matrix.unfix(variable);
    }

}

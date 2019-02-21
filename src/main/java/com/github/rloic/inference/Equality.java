package com.github.rloic.inference;

public class Equality implements Inference {

    private final int lhs;
    private final int rhs;

    public Equality(int lhs, int rhs) {
        this.lhs = lhs;
        this.rhs = rhs;
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        matrix.link(lhs, rhs);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        matrix.unlink(lhs);
    }

    @Override
    public String toString() {
        return "(var_" + lhs + "==var_" + rhs + ")";
    }
}

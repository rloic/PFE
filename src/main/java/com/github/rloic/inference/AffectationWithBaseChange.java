package com.github.rloic.inference;


import it.unimi.dsi.fastutil.ints.IntList;

public class AffectationWithBaseChange extends Affectation {

    private int pivot;
    private int newBaseVar;
    private IntList xorsVariable;
    private IntList xorsBase;

    public AffectationWithBaseChange(
            int variable,
            boolean value,
            int newBaseVar,
            int pivot,
            IntList xorsVariable,
            IntList xorsBase
    ) {
        super(variable, value);
        this.newBaseVar = newBaseVar;
        this.pivot = pivot;
        this.xorsVariable = xorsVariable;
        this.xorsBase = xorsBase;
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        matrix.fix(variable, value);
        for(int i = 0; i < xorsBase.size(); i++) {
            matrix.xor(pivot, xorsBase.getInt(i));
        }
        for(int i = 0; i < xorsVariable.size(); i++) {
            matrix.xor(xorsVariable.getInt(i), pivot);
        }
        matrix.swapBase(variable, newBaseVar);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        matrix.swapBase(newBaseVar, variable);
        for(int i = 0; i < xorsVariable.size(); i++) {
            matrix.xor(xorsVariable.getInt(i), pivot);
        }
        for(int i = 0; i < xorsBase.size(); i++) {
            matrix.xor(pivot, xorsBase.getInt(i));
        }
        matrix.unfix(variable);
    }
}

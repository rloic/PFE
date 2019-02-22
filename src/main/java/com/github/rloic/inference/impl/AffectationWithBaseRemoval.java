package com.github.rloic.inference.impl;

import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.util.Logger;

public class AffectationWithBaseRemoval extends Affectation {

    private final int pivot;

    public AffectationWithBaseRemoval(int variable, boolean value, int pivot) {
        super(variable, value);
        this.pivot = pivot;
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        Logger.debug("Fix var_" + variable + " to " + value);
        matrix.fix(variable, value);
        Logger.trace("\n" + matrix);
        Logger.debug("Remove var_" + variable + " from base");
        matrix.removeFromBase(variable);
        Logger.trace("\n" + matrix);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        Logger.debug("Add var_" + variable + " to base");
        matrix.appendToBase(variable, pivot);
        Logger.trace("\n" + matrix);
        Logger.debug("Unfix var_" + variable);
        matrix.unfix(variable);
        Logger.trace("\n" + matrix);
    }

}

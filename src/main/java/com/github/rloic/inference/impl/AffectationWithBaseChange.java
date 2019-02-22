package com.github.rloic.inference.impl;


import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.util.Logger;
import it.unimi.dsi.fastutil.ints.IntList;

public class AffectationWithBaseChange extends Affectation {

    private int pivot;
    private int newBaseVar;
    private IntList xorsVariable;

    public AffectationWithBaseChange(
            int variable,
            boolean value,
            int newBaseVar,
            int pivot,
            IntList xorsVariable
    ) {
        super(variable, value);
        this.newBaseVar = newBaseVar;
        this.pivot = pivot;
        this.xorsVariable = xorsVariable;
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        Logger.debug("Fix var_" + variable + " to " + value);
        matrix.fix(variable, value);
        Logger.trace("\n" + matrix);
        for(int i = 0; i < xorsVariable.size(); i++) {
            Logger.debug("row_" + xorsVariable.getInt(i) + " <- row_" + xorsVariable.getInt(i) + " xor row_" + pivot);
            matrix.xor(xorsVariable.getInt(i), pivot);
            Logger.trace("\n" + matrix);
        }
        Logger.debug("Remove var_" + variable + " from base and add var_" + newBaseVar + " to base");
        matrix.swapBase(variable, newBaseVar);
        Logger.trace("\n" + matrix);
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        Logger.debug("Remove var_" + newBaseVar + " from base and add var_" + variable + " to base");
        matrix.swapBase(newBaseVar, variable);
        Logger.trace("\n" + matrix);
        for(int i = 0; i < xorsVariable.size(); i++) {
            Logger.debug("row_" + xorsVariable.getInt(i) + " <- row_" + xorsVariable.getInt(i) + " xor row_" + pivot);
            matrix.xor(xorsVariable.getInt(i), pivot);
            Logger.trace("\n" + matrix);
        }
        Logger.trace("Unfix var_" + variable);
        matrix.unfix(variable);
        Logger.trace("\n" + matrix);
    }
}

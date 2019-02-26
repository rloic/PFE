package com.github.rloic.inference.impl;

import com.github.rloic.inference.*;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.exception.ContradictionException;

public class InferenceEngineImpl implements InferenceEngine {

    private boolean isEven(int x) {
        return x % 2 == 0;
    }

    @Override
    public Inferences inferAndUpdate(InferenceMatrix matrix) {
        Inferences steps = new Inferences();
        Inferences newStep;
        do {
            newStep = new Inferences();
            for (int row = 0; row < matrix.rows(); row++) {
                int nbTrues = 0;
                int col = 0;
                while (col < matrix.cols() && !matrix.isUnknown(row, col)) {
                    if (matrix.isTrue(row, col)) {
                        nbTrues += 1;
                    }
                    col += 1;
                }
                // If there is a least one unknown
                if (col != matrix.cols()) {
                    int firstUnknown = col;
                    col = matrix.cols() - 1;
                    // We are looking for the second unknown
                    while (col >= 0 && !matrix.isUnknown(row, col)) {
                        if (matrix.isTrue(row, col)) {
                            nbTrues += 1;
                        }
                        col -= 1;
                    }
                    // If the second unknown is different from the first one
                    if (col > firstUnknown) {
                        int lastUnknown = col;
                        col = firstUnknown + 1;
                        // We are looking if they are more unknowns
                        while (col < lastUnknown && !matrix.isUnknown(row, col)) {
                            if (matrix.isTrue(row, col)) {
                                nbTrues += 1;
                            }
                            col += 1;
                        }
                        // If there are no more unknowns (nb unknowns == 2)
                        if (col == lastUnknown) {
                            // If there is no activate true in the getRow and we didn't know that
                            // the two variables where equivalent
                            if (nbTrues == 0 && !matrix.isEquivalent(firstUnknown, lastUnknown)) {
                                newStep.add(new Equality(firstUnknown, lastUnknown));
                            }
                        }
                        // If the second unknown is the first one
                    } else if (nbTrues <= 1) {
                        newStep.add(createAffectation(matrix, firstUnknown, !isEven(nbTrues)));
                    }
                }
            }
            if (!newStep.isEmpty()) {
                newStep.apply(matrix);
                steps.add(newStep);
            }
        } while (!newStep.isEmpty());

        return steps;
    }

    @Override
    public Affectation createAffectation(InferenceMatrix matrix, int variable, boolean value) {
        if (matrix.isBase(variable)) {
            int pivot = matrix.pivotOf(variable);
            int newBaseVariable = 0;
            while (newBaseVariable < matrix.cols() && (matrix.isBase(newBaseVariable) || !matrix.isUnknown(pivot, newBaseVariable))) {
                newBaseVariable += 1;
            }

            if (newBaseVariable == matrix.cols()) {
                return new AffectationWithBaseRemoval(variable, value, pivot);
            } else {
                IntList xors = matrix.rowsWhereUnknown(newBaseVariable);
                xors.rem(pivot);
                return new AffectationWithBaseChange(variable, value, newBaseVariable, pivot, xors);
            }
        } else {
            return new Affectation(variable, value);
        }
    }
}

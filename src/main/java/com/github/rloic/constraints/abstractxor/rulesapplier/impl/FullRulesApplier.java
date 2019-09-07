package com.github.rloic.constraints.abstractxor.rulesapplier.impl;

import com.github.rloic.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.dancinglinks.actions.IUpdater;
import com.github.rloic.dancinglinks.actions.UpdaterList;
import com.github.rloic.dancinglinks.actions.impl.*;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.RulesApplier;
import com.github.rloic.paper.dancinglinks.actions.impl.*;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;
import it.unimi.dsi.fastutil.Function;

public class FullRulesApplier implements RulesApplier {

    private final InferenceEngine engine;

    public FullRulesApplier() {
        this.engine = new FullInferenceEngine();
    }

    FullRulesApplier(InferenceEngine engine) {
        this.engine = engine;
    }

    public IUpdater buildTrueAssignation(int variable) {
        return fix(variable, true)
                .then(matrix -> {
                    if (matrix.isBase(variable)) {
                        int pivot = matrix.pivotOf(variable);
                        return inferOnlyForEquation(pivot)
                                .then(inferBasesEqualities(pivot));
                    } else {
                        return inferForAllEquationsOf(matrix, variable);
                    }
                });
    }

    /* R1:
     * if ∃variable ∈ HB such as D(Δ_{variable}) = {false} -> remove the column of the variable
     *
     * R2:
     * if ∃variable ∈ B such as D(Δ_{variable}) = {false} ->
     * let p = pivot(variable)
     * if vars_{p} = {variable} then remove the column of the variable and the row of the equation
     * else choose a new base and perform a pivot
     */
    public IUpdater buildFalseAssignation(int variable) {
        // D(Δ_{variable}) = { false }
        return fix(variable, false)
                .then(matrix -> {
                    // variable ∈ B
                    if (matrix.isBase(variable)) {
                        int pivot = matrix.pivotOf(variable);
                        return inferOnlyForEquation(pivot)
                                .then(
                                        matrix.isEmpty(pivot) ?
                                                removeEquation(pivot) :
                                                makePivot(matrix, pivot, variable)
                                );
                    } else {
                        return inferForAllEquationsOf(matrix, variable);
                    }
                }).then(removeVariable(variable))
                .then(inferAllBaseEqualities(variable));
    }

    /* Propagation of B <- 1
     *  A   B   C   D     A   B   C   D
     * (x)      1   1 => (1)      1   1
     *     (1)  1   1        (1)  1   1
     */
    private Function<IDancingLinksMatrix, IUpdater> inferBasesEqualities(int pivot) {
        return matrix -> {
            IDancingLinksMatrix m = (IDancingLinksMatrix) matrix;
            int baseVar = m.baseVariableOf(pivot);
            assert baseVar != -1;
            UpdaterList sameVar = new UpdaterList("InferBasesEqualities");
            if (m.isTrue(baseVar)) {
                int firstOffBase = m.firstOffBase(pivot);
                inferThatOtherBaseAreEqualsToThisBase(m, sameVar, pivot, firstOffBase);
            }
            return sameVar;
        };
    }

    final IUpdater inferOnlyForEquation(int equation) {
        return infer(equation);
    }

    final IUpdater inferForAllEquationsOf(IDancingLinksMatrix matrix, int variable) {
        UpdaterList updaterList = new UpdaterList("InferForAllEquations");
        for (int equation : matrix.equationsOf(variable)) {
            updaterList.addUncommitted(infer(equation));
        }
        return updaterList;
    }

    final IUpdater xorAndInferAllEquationsOf(IDancingLinksMatrix matrix, int pivot, int variable) {
        UpdaterList updaterList = new UpdaterList("XorAndInferForAllEquations");
        for (int equation : matrix.equationsOf(variable)) {
            if (equation != pivot) {
                updaterList.addUncommitted(xor(equation, pivot));
                updaterList.addUncommitted(infer(equation));
            }
        }
        return updaterList;
    }

    final IUpdater makePivot(IDancingLinksMatrix matrix, int pivot, int oldBaseVar) {
        int newBaseVar = matrix.eligibleBase(pivot);
        return new SwapBase(oldBaseVar, newBaseVar)
                .then(m -> xorAndInferAllEquationsOf(m, pivot, newBaseVar));
    }

    final Function<IDancingLinksMatrix, IUpdater> inferAllBaseEqualities(int variable) {
        return matrix -> {
            IDancingLinksMatrix m = (IDancingLinksMatrix) matrix;
            UpdaterList updaters = new UpdaterList();

            for (int pivot : m.equationsOf(variable)) {
                int base = m.baseVariableOf(pivot);
                int firstOffBase = m.firstOffBase(pivot);
                if (firstOffBase != -1) {
                    if (m.isTrue(base)) {
                        inferThatOtherBaseAreEqualsToThisBase(m, updaters, pivot, firstOffBase);
                    } else {
                        for (int target : m.equationsOf(firstOffBase)) {
                            int targetBaseVar = m.baseVariableOf(target);
                            if (
                                    targetBaseVar != -1
                                            && m.isTrue(targetBaseVar)
                                            && m.nbUnknowns(target) == m.nbUnknowns(pivot) - 1
                                            && m.nbTrues(target) == m.nbTrues(pivot) + 1
                                            && m.sameOffBaseVariables(target, pivot)
                            ) {
                                updaters.addUncommitted(propagation(base, true));
                                break;
                            }
                        }
                    }
                }
            }
            return updaters;
        };
    }

    private void inferThatOtherBaseAreEqualsToThisBase(IDancingLinksMatrix m, UpdaterList updaters, int pivot, int firstOffBase) {
        for (int target : m.equationsOf(firstOffBase)) {
            int targetBaseVar = m.baseVariableOf(target);
            assert targetBaseVar != -1;
            if (
                    !m.isTrue(targetBaseVar)
                  && m.nbUnknowns(target) == m.nbUnknowns(pivot) + 1
                  && m.nbTrues(target) == m.nbTrues(pivot) - 1
                  && m.sameOffBaseVariables(target, pivot)
            ) {
                updaters.addUncommitted(propagation(targetBaseVar, true));
            }
        }
    }

    final IUpdater removeEquation(int equation) {
        return new RemoveEquation(equation);
    }

    final IUpdater fix(int variable, boolean value) {
        return new Fix(variable, value);
    }

    final IUpdater removeVariable(int variable) {
        return new RemoveVariable(variable);
    }

    final IUpdater infer(int equation) {
        return new InferFromEquation(engine, equation);
    }

    final IUpdater propagation(int variable, boolean value) {
        return new InferAffectation(variable, value);
    }

    final IUpdater xor(int target, int pivot) {
        return new XOR(target, pivot);
    }


}
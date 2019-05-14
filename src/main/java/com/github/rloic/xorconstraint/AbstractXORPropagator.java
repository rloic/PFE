package com.github.rloic.xorconstraint;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.*;
import com.github.rloic.paper.dancinglinks.impl.DancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import it.unimi.dsi.fastutil.ints.IntSet;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorDownBranch;
import org.chocosolver.solver.search.loop.monitors.IMonitorUpBranch;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import java.util.*;

import static com.github.rloic.paper.dancinglinks.actions.UpdaterState.DONE;

/**
 * The propagator for the global constraint abstract XOR
 * The abstract xor operation is defined by the truth table
 * <p>
 * A     B     A absXor B
 * 0     0     0
 * 0     1     1
 * 1     0     1
 * 1     0     0 or 1
 */
public class AbstractXORPropagator extends Propagator<BoolVar> implements IMonitorUpBranch, IMonitorDownBranch {

    /* The solver */
    private final Solver solver;

    /* The commands applied on the state matrix */
    private final Stack<UpdaterList> commands;

    /* The matrix that represents the abstract xor system */
    public final IDancingLinksMatrix matrix;

    /* The variable column of a BoolVar in the matrix */
    public final Map<BoolVar, Integer> indexOf;

    /* The inference engine (it computes implications) */
    private final InferenceEngine engine;

    /* The rules applier (it manipulates the matrix) */
    private final RulesApplier rulesApplier;

    public AbstractXORPropagator(
            BoolVar[] vars,
            BoolVar[][] xors,
            InferenceEngine engine,
            RulesApplier rulesApplier,
            Solver solver
    ) {
        super(vars, PropagatorPriority.CUBIC, true);

        this.commands = new Stack<>();
        commands.push(new UpdaterList());

        this.engine = engine;
        this.rulesApplier = rulesApplier;
        this.solver = solver;

        indexOf = new HashMap<>();
        int lastIndex = 0;
        for (BoolVar variable : vars) {
            indexOf.put(variable, lastIndex++);
        }
        int[][] equations = new int[xors.length][];
        for (int i = 0; i < xors.length; i++) {
            final int length = xors[i].length;
            equations[i] = new int[length];
            for (int j = 0; j < length; j++) {
                equations[i][j] = indexOf.get(xors[i][j]);
            }
        }
        matrix = new DancingLinksMatrix(equations, lastIndex);
        solver.plugMonitor(this);
    }

    @Override
    public int arity() {
        return matrix.numberOfUndefinedVariables();
    }

    @Override
    public void propagate(int evtmask) {
        RulesApplier.gauss(matrix);
        assert checkState(matrix);
        List<Propagation> propagations = new ArrayList<>();
        for (int equation : matrix.activeEquations()) {
            propagations.addAll(engine.infer(matrix, equation));
        }

        IUpdater updater;
        for (int i = 0; i < propagations.size(); i++) {
            Propagation inference = propagations.get(i);
            int variable = inference.variable;
            boolean value = inference.value;

            if (matrix.isUndefined(variable)) {
                updater = onPropagate(variable, value);
                UpdaterState status = updater.update(matrix, propagations);
                assert status == DONE;
            }
            assert (matrix.isTrue(variable) && value) || (matrix.isFalse(variable) && !value);
        }

        assert checkState(matrix);

        for (int idxVarInProp = 0; idxVarInProp < vars.length; idxVarInProp++) {
            if (vars[idxVarInProp].isInstantiated()) {
                try {
                    propagations.addAll(synchronize(idxVarInProp));
                } catch (ContradictionException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        assert checkState(matrix);

        for (Propagation propagation : propagations) {
            try {
                propagation.propagate(vars, this);
            } catch (ContradictionException contradiction) {
                throw new RuntimeException(contradiction);
            }
        }

    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        List<Propagation> propagations = synchronize(idxVarInProp);
        for (Propagation propagation : propagations) {
            // Will propagate to inferences through Choco
            propagation.propagate(vars, this);
        }
    }

    private List<Propagation> synchronize(int idxVarInProp) throws ContradictionException {
        Affectation externalAffectation = new Affectation(idxVarInProp, isTrue(vars[idxVarInProp]));
        if (!matrix.isUndefined(idxVarInProp)) {
            if (isIncoherent(idxVarInProp)) {
                failBecauseOf(idxVarInProp, externalAffectation);
            } else {
                return Collections.emptyList();
            }
        }

        UpdaterList step = commands.peek();
        List<Propagation> propagations = new ArrayList<>();
        IUpdater updater = onPropagate(idxVarInProp, isTrue(vars[idxVarInProp]));
        UpdaterState state = updater.update(matrix, propagations);

        switch (state) {
            case DONE:
                step.addCommitted(updater);
                break;
            case EARLY_FAIL:
                failBecauseOf(idxVarInProp, externalAffectation);
            case LATE_FAIL:
                updater.restore(matrix);
                failBecauseOf(idxVarInProp, externalAffectation);
        }

        for (int i = 0; i < propagations.size(); i++) {
            int variable = propagations.get(i).variable;
            boolean value = propagations.get(i).value;

            if (matrix.isUndefined(variable)) {
                updater = onPropagate(variable, value);
                if (updater.update(matrix, propagations) != DONE) {
                    throw new RuntimeException("Incoherent inference for " + updater.toString());
                }
                step.addCommitted(updater);
            } else { // the variable is already instantiated by Choco
                if (isIncoherent(variable)) {
                    failBecauseOf(variable, externalAffectation);
                }
            }

            for (int equation : matrix.activeEquations()) {
                if (matrix.nbUnknowns(equation) == 1) {
                    propagations.addAll(new FullInferenceEngine().infer(matrix, equation));
                }
            }

        }

        assert checkState(matrix);
        return propagations;
    }

    @Override
    public ESat isEntailed() {
        IntSet unassignedVars = matrix.unassignedVars();

        boolean failsOnSynchronize = false;
        for (int variable : unassignedVars) {
            try {
                synchronize(variable);
            } catch (ContradictionException unused) {
                failsOnSynchronize = true;
                break;
            }
        }

        if (failsOnSynchronize) return ESat.FALSE;
        return ESat.TRUE;
    }

    private boolean isIncoherent(int variable) {
        return (matrix.isTrue(variable) && isFalse(vars[variable])) || (matrix.isFalse(variable) && isTrue(vars[variable]));
    }

    private void failBecauseOf(int variable, Affectation affectation) throws ContradictionException {
        throw new ContradictionException().set(this, vars[variable], "Invalid: " + affectation.toString());
    }

    private IUpdater onPropagate(int variable, boolean value) {
        if (value) {
            return rulesApplier.buildTrueAssignation(variable);
        } else {
            return rulesApplier.buildFalseAssignation(variable);
        }
    }

    private boolean isTrue(BoolVar variable) {
        return variable.isInstantiated() && variable.getValue() == 1;
    }

    private boolean isFalse(BoolVar variable) {
        return variable.isInstantiated() && variable.getValue() == 0;
    }

    private boolean checkState(IDancingLinksMatrix m) {
        return atLeastTwoVarsPerLine(m)
                && twoVarsAndOneAtTrueImpliesOtherAtTrue(m)
                && basesEqualities(m)
                && isNormalForm(m);
    }

    private boolean atLeastTwoVarsPerLine(IDancingLinksMatrix m) {
        for (int equation : m.activeEquations()) {
            int count = 0;
            for (int unused : m.variablesOf(equation)) {
                count += 1;
            }
            if (count < 2) {
                return false;
            }
        }
        return true;
    }

    private boolean twoVarsAndOneAtTrueImpliesOtherAtTrue(IDancingLinksMatrix m) {
        for (int equation : m.activeEquations()) {
            int count = 0;
            int nbTrues = 0;
            for (int variable : m.variablesOf(equation)) {
                count += 1;
                if (m.isTrue(variable)) {
                    nbTrues += 1;
                }
            }
            if (count == 2 && nbTrues == 1) {
                return false;
            }
        }
        return true;
    }

    private boolean basesEqualities(IDancingLinksMatrix m) {
        for (int equation : m.activeEquations()) {
            int baseVar = m.baseVariableOf(equation);
            if (baseVar != -1 && m.isTrue(baseVar)) {
                for (int equationJ : m.activeEquations()) {
                    if (equation != equationJ && m.sameOffBaseVariables(equation, equationJ)) {
                        if (!m.isTrue(m.baseVariableOf(equationJ))) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    private boolean isNormalForm(IDancingLinksMatrix m) {
        for (int equation : m.activeEquations()) {
            int baseVar = m.baseVariableOf(equation);
            if (baseVar != -1) {
                int count = 0;
                for (int unused : m.equationsOf(baseVar)) {
                    count += 1;
                }
                if (count != 1) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void beforeDownBranch(boolean left) {
        commands.add(new UpdaterList());
    }

    @Override
    public void beforeUpBranch() {
        commands.pop().restore(matrix);
    }
}

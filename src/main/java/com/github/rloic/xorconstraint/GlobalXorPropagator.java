package com.github.rloic.xorconstraint;

import com.github.rloic.inference.InferenceEngine;
import com.github.rloic.inference.InferenceMatrix;
import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.DenseMatrix;
import com.github.rloic.inference.impl.InferenceEngineImpl;
import com.github.rloic.inference.impl.Inferences;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

import java.util.*;

public class GlobalXorPropagator extends Propagator<BoolVar> {

    private final Map<BoolVar, Integer> indexOf = new HashMap<>();
    private final InferenceMatrix matrix;
    private final InferenceEngine engine = new InferenceEngineImpl();

    public GlobalXorPropagator(BoolVar[] variables, BoolVar[][] xors) {
        super(variables, PropagatorPriority.UNARY, true);
        int lastIndex = 0;
        for (BoolVar variable : variables) {
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
        matrix = new DenseMatrix(equations, variables.length);
    }

    private static int nbCall = 0;
    private List<Inferences> stack = new ArrayList<>();

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.combine(IntEventType.REMOVE);
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        Logger.debug("\n\n\n***** Call " + nbCall++ + " *****");
        while (matrix.isFixed(idxVarInProp)) {
            rollback();
        }
        Logger.debug("BoolVars[]: \t" + Arrays.toString(vars));
        Inferences inferences = new Inferences();
        Inferences affectations = new Inferences();
        BoolVar var = vars[idxVarInProp];
        for (int varIdx : matrix.equivalents(idxVarInProp)) {
            Affectation affectation = engine.createAffectation(matrix, varIdx, var.getValue() == 1);
            try {
                affectation.apply(matrix);
            } catch (IllegalStateException e) {
                Logger.err(e);
                throw new ContradictionException();
            }
            affectations.add(affectation);
        }
        Logger.debug("Choco affectations: " + affectations);
        inferences.addAll(affectations);
        inferences.addAll(engine.inferAndUpdate(matrix));
        Logger.debug("Infers => " + inferences);
        stack.add(inferences);
        inferences.constraint(vars, this);
        Logger.debug("Matrix state: \t" + matrix.varsToString());
    }

    @Override
    public void propagate(int evtmask) {
    }

    void rollback() {
        stack.remove(stack.size() - 1).unapply(matrix);
    }

    @Override
    public ESat isEntailed() {
        Logger.debug("call entailed");
        if (matrix.isAllFixed()) {
            return ESat.TRUE;
        } else {
            return ESat.UNDEFINED;
        }
    }
}

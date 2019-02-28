package com.github.rloic.xorconstraint;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.InferenceEngine;
import com.github.rloic.paper.XORMatrix;
import com.github.rloic.paper.impl.InferenceEngineImpl;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import com.github.rloic.paper.impl.NaiveMatrixImplWithBoolVar;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalXorPropagatorChoco extends Propagator<BoolVar> {

    private final XORMatrix matrix;
    private final InferenceEngine engine = new InferenceEngineImpl();
    private final BoolVar[][] xors;

    public GlobalXorPropagatorChoco(BoolVar[] variables, BoolVar[][] xors) {
        super(variables, PropagatorPriority.CUBIC, true);

        final Map<BoolVar, Integer> indexOf = new HashMap<>();
        int lastIndex = 0;
        for (BoolVar variable : variables) {
            indexOf.put(variable, lastIndex++);
        }

        BoolVar[][] equations = new BoolVar[xors.length][];
        for (int i = 0; i < xors.length; i++) {
            equations[i] = new BoolVar[variables.length];
            for (int j = 0; j < xors[i].length; j++) {
                equations[i][indexOf.get(xors[i][j])] = variables[indexOf.get(xors[i][j])];
            }
        }
        this.xors = xors;
        matrix = new NaiveMatrixImplWithBoolVar(variables, equations, variables.length, this);
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        XORMatrix.normalize(matrix);
        List<Affectation> affectations = engine.applyAndInfer(matrix, new Affectation(idxVarInProp, vars[idxVarInProp].getValue() == 1));
        while (!affectations.isEmpty()) {
            Affectation head = affectations.remove(0);
            affectations.addAll(engine.applyAndInfer(matrix, head));
        }
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        XORMatrix.normalize(matrix);
    }

    @Override
    public ESat isEntailed() {
        Logger.debug("Call isEntailed");
        for (BoolVar var : vars) {
            if (!var.isInstantiated()) return ESat.UNDEFINED;
        }
        for (BoolVar[] xor : xors) {
            if (sum(xor) == 1) return ESat.FALSE;
        }
        return ESat.TRUE;
    }

    private int sum(BoolVar[] variables) {
        int sum = 0;
        for (int j = 0; j < variables.length; j++) {
            if (variables[j].getValue() == 1) {
                sum += 1;
            }
        }
        return sum;
    }

}

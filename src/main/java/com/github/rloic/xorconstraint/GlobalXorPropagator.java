package com.github.rloic.xorconstraint;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.Algorithms;
import com.github.rloic.paper.InferenceEngine;
import com.github.rloic.paper.XORMatrix;
import com.github.rloic.paper.impl.InferenceEngineImpl;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalXorPropagator extends Propagator<BoolVar> {

    private final int[][] equations;

    public GlobalXorPropagator(BoolVar[] variables, BoolVar[][] xors) {
        super(variables, PropagatorPriority.CUBIC, true);
        final Map<BoolVar, Integer> indexOf = new HashMap<>();
        int lastIndex = 0;
        for (BoolVar variable : variables) {
            indexOf.put(variable, lastIndex++);
        }
        equations = new int[xors.length][];
        for (int i = 0; i < xors.length; i++) {
            final int length = xors[i].length;
            equations[i] = new int[length];
            for (int j = 0; j < length; j++) {
                equations[i][j] = indexOf.get(xors[i][j]);
            }
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return IntEventType.all();
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        XORMatrix matrix = new NaiveMatrixImpl(equations, vars.length);
        for(int j = 0; j < vars.length; j++) {
            if (vars[j].isInstantiated()) {
                matrix.fix(j, vars[j].getValue() == 1);
            }
        }
        List<Affectation> affectations = new ArrayList<>();
        if (!Algorithms.normalize(matrix, affectations)) {
            throw new ContradictionException();
        }
        for(Affectation affectation : affectations) {
            if(affectation.value) {
                vars[affectation.variable].setToTrue(this);
            } else {
                vars[affectation.variable].setToFalse(this);
            }
        }
    }

    @Override
    public void propagate(int evtmask) {
    }


    @Override
    public ESat isEntailed() {
        XORMatrix matrix = new NaiveMatrixImpl(equations, vars.length);
        for(int j = 0; j < vars.length; j++) {
            if (vars[j].isInstantiated()) {
                matrix.fix(j, vars[j].getValue() == 1);
            }
        }
        List<Affectation> affectations = new ArrayList<>();
        if (!Algorithms.normalize(matrix, affectations)) {
            return ESat.FALSE;
        }

        if (!affectations.isEmpty()) {
            return ESat.UNDEFINED;
        }
        return ESat.TRUE;
    }
}

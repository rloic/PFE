package com.github.rloic.xorconstraint;

import com.github.rloic.inference.Inference;
import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.Inferences;
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
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

import java.util.*;

public class GlobalXorPropagator extends Propagator<BoolVar> {

    private final XORMatrix matrix;
    private final InferenceEngine engine = new InferenceEngineImpl();

    public GlobalXorPropagator(BoolVar[] variables, BoolVar[][] xors) {
        super(variables, PropagatorPriority.CUBIC, true);
        final Map<BoolVar, Integer> indexOf = new HashMap<>();
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
        matrix = new NaiveMatrixImpl(equations, variables.length);
    }

    private static int nbCall = 0;

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        BoolVar var = vars[idxVarInProp];
        Affectation chocoAffectation = new Affectation(idxVarInProp, var.getValue() == 1);
        Logger.debug("\n\n\n***** Call " + nbCall++ + ": (" + var.getName() + "<-" + (var.getValue() == 1) + ") <=> " + chocoAffectation);
        while (matrix.isFixed(idxVarInProp)) {
            matrix.rollback();
        }
        List<Affectation> affectations;
        affectations = engine.applyAndInfer(matrix, chocoAffectation);

        Logger.debug("ChocoVars: \t" + Arrays.toString(vars));
        Logger.debug("Interval:  \t" + interalVarsState());
        Logger.trace(matrix);

        while (!affectations.isEmpty()) {
            Affectation head = affectations.remove(0);
            Logger.debug("Infers => (" + vars[head.variable].getName() + "<-" + head.value + ") <=> " + head);
            List<Affectation> inferences;
            inferences = engine.applyAndInfer(matrix, head);
            affectations.addAll(inferences);
            head.constraint(vars, this);
        }
    }

    @Override
    public void propagate(int evtmask) {
        Logger.trace("Init");
    }

    private String interalVarsState() {
        StringBuilder stringBuilder = new StringBuilder("[");
        for (int i = 0; i < matrix.cols(); i++) {
            stringBuilder.append(vars[i].getName())
                    .append(" = ");
            if (matrix.isFixed(i)) {
                if (matrix.isFixedToTrue(i)) {
                    stringBuilder.append("1");
                } else {
                    stringBuilder.append("0");
                }
            } else {
                stringBuilder.append("[0,1]");
            }
            stringBuilder.append(", ");
        }
        if (matrix.cols() != 0) {
            stringBuilder.setLength(stringBuilder.length() - 2);
        }
        stringBuilder.append("]");
        return stringBuilder.toString();
    }

    @Override
    public ESat isEntailed() {
        for (int i = 0; i < matrix.rows(); i++) {
            if (matrix.nbUnknowns(i) != 0) {
                return ESat.UNDEFINED;
            } else if (matrix.nbTrues(i) == 1) {
                return ESat.FALSE;
            }
        }
        return ESat.TRUE;
    }
}

package com.github.rloic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.github.rloic.paper.Algorithms;
import com.github.rloic.paper.XORMatrix;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import com.github.rloic.util.Logger;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import org.chocosolver.memory.IStateBitSet;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;


public class ToyExample {

    public static void main(String[] args) {
        XORMatrix matrix =new NaiveMatrixImpl(new int[][]{
              new int[]{0, 1}
        }, 3);

        matrix.fix(0, false);
        Algorithms.normalize(matrix, new ArrayList<>());
        Algorithms.propagateVarAssignedToTrue(matrix, 0, new ArrayList<>());
        System.out.println(matrix);
    }

    static void solve(Model model, BoolVar[] variables) {
        Solver solver = model.getSolver();
        while (solver.solve()) {
            printSBoxes(variables);
        }
        solver.printShortStatistics();
    }

    private static void printSBoxes(BoolVar[] sBoxes) {
        List<Integer> values = Arrays.stream(sBoxes)
                .map(IntVar::getValue)
                .collect(Collectors.toList());
        System.out.println("Solution: " + values);
    }

}

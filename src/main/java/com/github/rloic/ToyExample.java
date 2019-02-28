package com.github.rloic;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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

    static void solve(Model model, BoolVar[] variables) {
        Solver solver = model.getSolver();
        while (solver.solve()) {
            printSBoxes(variables);
        }
        solver.printShortStatistics();
    }



    public static void main(String[] args) {
        Model modelGlobalXor = new Model();
        BoolVar[] varsGlobalXor = modelGlobalXor.boolVarArray(7);
        modelGlobalXor.post(new Constraint("MyConstraint", new GlobalXorPropagator(varsGlobalXor, new BoolVar[][] {
                new BoolVar[] {varsGlobalXor[0], varsGlobalXor[1], varsGlobalXor[2]},
                new BoolVar[] {varsGlobalXor[2], varsGlobalXor[3], varsGlobalXor[4]},
                new BoolVar[] {varsGlobalXor[4], varsGlobalXor[5], varsGlobalXor[6]}
        })));

        solve(modelGlobalXor, varsGlobalXor);

        Model modelMultiXor = new Model();
        BoolVar[] varsMultiXor = modelMultiXor.boolVarArray(7);
        modelMultiXor.sum(new IntVar[]{varsMultiXor[0], varsMultiXor[1], varsMultiXor[2]}, "!=", 1).post();
        modelMultiXor.sum(new IntVar[]{varsMultiXor[2], varsMultiXor[3], varsMultiXor[4]}, "!=", 1).post();
        modelMultiXor.sum(new IntVar[]{varsMultiXor[4], varsMultiXor[5], varsMultiXor[6]}, "!=", 1).post();

        solve(modelMultiXor, varsMultiXor);
    }

    private static void printSBoxes(BoolVar[] sBoxes) {
        List<Integer> values = Arrays.stream(sBoxes)
                .map(IntVar::getValue)
                .collect(Collectors.toList());
        System.out.println("Solution: " + values);
    }

}

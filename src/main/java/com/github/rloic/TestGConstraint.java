package com.github.rloic;

import com.github.rloic.util.Logger;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.stream.Collectors;

import static com.github.rloic.util.Logger.DebugLogger.DEBUG;
import static com.github.rloic.util.Logger.InfoLogger.INFO;
import static com.github.rloic.util.Logger.TraceLogger.TRACE;

public class TestGConstraint {

    public static void main(String[] args) {
        Logger.level(INFO);
        Model m = new Model();

        BoolVar[] variables = new BoolVar[7];
        for (int i = 0; i < variables.length; i++) {
            variables[i] = m.boolVar();
        }
        BoolVar[][] xors = new BoolVar[][]{
                new BoolVar[]{variables[2], variables[5], variables[6]},
                new BoolVar[]{variables[4], variables[3], variables[5]},
                new BoolVar[]{variables[1], variables[4], variables[2]},
                new BoolVar[]{variables[1], variables[3], variables[0]},
        };
        Constraint globalXor = new Constraint("GlobalXor", new GlobalXorPropagator(variables, xors));
        m.post(globalXor);

        Solver solver = m.getSolver();
        while (solver.solve()) {
            Logger.info(Arrays.stream(variables).map(IntVar::getValue).collect(Collectors.toList()));
        }
        solver.printShortStatistics();

    }

}

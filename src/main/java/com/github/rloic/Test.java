package com.github.rloic;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.InferenceEngine;
import com.github.rloic.paper.XORMatrix;
import com.github.rloic.paper.impl.InferenceEngineImpl;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Test {

    public static void main(String[] args) throws IOException, ContradictionException {
        int A = 0;
        int B = 1;
        int C = 2;
        int D = 3;
        int E = 4;
        int F = 5;
        int G = 6;

        Model m = new Model();
        BoolVar[] variables = m.boolVarArray(4);
        m.post(new Constraint("GlobalXor", new GlobalXorPropagator(variables, new BoolVar[][] {
                new BoolVar[]{variables[A], variables[C], variables[D]},
                new BoolVar[]{variables[C], variables[D], variables[B]},
        })));

        Solver solver = m.getSolver();
        while (solver.solve()) {
            System.out.println(Arrays.toString(variables));
        }
        solver.printShortStatistics();

    }

}

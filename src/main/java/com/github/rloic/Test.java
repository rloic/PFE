package com.github.rloic;

import com.github.rloic.paper.dancinglinks.Algorithms;
import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.impl.Fix;
import com.github.rloic.paper.dancinglinks.impl.DancingLinksMatrix;
import com.github.rloic.util.Logger;
import com.github.rloic.xorconstraint.BasePropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

import static com.github.rloic.util.Logger.DebugLogger.DEBUG;

public class Test {

   static final int A = 0;
   static final int B = 1;
   static final int C = 2;
   static final int D = 3;
   static final int A_B = 4;
   static final int A_C = 5;
   static final int A_D = 6;
   static final int B_C = 7;
   static final int B_D = 8;
   static final int C_D = 9;

   static int[] xor(int... elements) {
      return elements;
   }

   public static void main(String[] args) {
      Logger.level(DEBUG);
      int lastUsedLetter = D;
      int[][] xors = new int[][]{
            xor(A, C, D),
            xor(B, C, D)

      };

      int nbVariables = lastUsedLetter + 1;
      runGlobalXor(nbVariables, xors);
      runSumXor(nbVariables, xors);
   }

   private static void runGlobalXor(
         int nbVariables,
         int[][] xors
   ) {
      Model m = new Model();
      Solver solver = m.getSolver();
      BoolVar[] variables = m.boolVarArray(nbVariables);

      BoolVar[][] xorBoolVar = new BoolVar[xors.length][];
      for (int i = 0; i < xors.length; i++) {
         xorBoolVar[i] = new BoolVar[xors[i].length];
         for (int j = 0; j < xors[i].length; j++) {
            xorBoolVar[i][j] = variables[xors[i][j]];
         }
      }

      m.post(new Constraint("GlobalXOR", new BasePropagator(
            variables,
            xorBoolVar,
            solver
      )));

      while (solver.solve()) {
         System.out.println(
               Arrays.stream(variables)
                     .map(IntVar::getValue)
                     .collect(Collectors.toList())
         );
      }
      solver.printShortStatistics();
   }

   private static void runSumXor(
         int nbVariables,
         int[][] xors
   ) {
      Model m = new Model();
      Solver solver = m.getSolver();
      BoolVar[] variables = m.boolVarArray(nbVariables);

      for (int[] xor : xors) {
         IntVar[] xorElements = new IntVar[xor.length];
         for (int j = 0; j < xor.length; j++) {
            xorElements[j] = variables[xor[j]];
         }
         m.sum(xorElements, "!=", 1).post();
      }

      while (solver.solve()) {
         System.out.println(
               Arrays.stream(variables)
                     .map(IntVar::getValue)
                     .collect(Collectors.toList())
         );
      }
      solver.printShortStatistics();
   }

}

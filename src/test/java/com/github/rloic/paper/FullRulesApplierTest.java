package com.github.rloic.paper;

import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.util.Logger;
import com.github.rloic.util.Pair;
import com.github.rloic.xorconstraint.AbstractXORPropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static com.github.rloic.util.Logger.TraceLogger.TRACE;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FullRulesApplierTest {

   private static int A = 0;
   private static int B = 1;
   private static int C = 2;
   private static int D = 3;
   private static int E = 4;
   private static int F = 5;
   private static int G = 6;
   private static int H = 7;
   private static int I = 8;

   @Test
   void should_infer_a_equals_b() {
      Pair<Set<String>, Set<String>> results = solveSystem(D + 1, new int[][]{
            new int[]{A, C, D},
            new int[]{B, C, D}
      });

      Set<String> naive = results._0;
      Set<String> gXor = results._1;

      assertTrue(naive.containsAll(gXor));
   }

   @Test
   void should_pass_3_elements_xor() {
      Pair<Set<String>, Set<String>> results = solveSystem(C + 1, new int[][]{
            new int[]{A, B, C}
      });

      Set<String> naive = results._0;
      Set<String> gXor = results._1;

      assertTrue(naive.containsAll(gXor));
   }

   @Test
   void should_pass_5_elements_xor() {
      Pair<Set<String>, Set<String>> results = solveSystem(F + 1, new int[][]{
            new int[]{A, B, C},
            new int[]{D},
            new int[]{E, F}
      });

      Set<String> naive = results._0;
      Set<String> gXor = results._1;

      assertTrue(naive.containsAll(gXor));
   }

   @Test
   void should_pass_test4() {
      Pair<Set<String>, Set<String>> results = solveSystem(E + 1, new int[][]{
            new int[]{A, C, D},
            new int[]{B, C, E}
      });

      Set<String> naive = results._0;
      Set<String> gXor = results._1;

      assertTrue(naive.containsAll(gXor));
   }

   @Test
   void should_pass_test5() {
      Pair<Set<String>, Set<String>> results = solveSystem(G + 1, new int[][]{
            new int[]{A, B, C},
            new int[]{C, D, E},
            new int[]{E, F, G}
      });

      Set<String> naive = results._0;
      Set<String> gXor = results._1;

      assertTrue(naive.containsAll(gXor));
   }

   @Test
   void should_pass_test6() {
      Logger.level(TRACE);
      Pair<Set<String>, Set<String>> results = solveSystem(F + 1, new int[][]{
            new int[]{A, C, E, F},
            new int[]{B, C, E, F},
            new int[]{D, E, F}
      });

      Set<String> naive = results._0;
      Set<String> gXor = results._1;

      assertTrue(naive.containsAll(gXor));
   }

   private Set<String> collectSolutions(Model m, BoolVar[] vars) {
      Solver s = m.getSolver();
      Set<String> solutions = new HashSet<>();
      while (s.solve()) {
         solutions.add(Arrays.stream(vars).map(IntVar::getValue).collect(Collectors.toList()).toString());
      }
      return solutions;
   }

   private Pair<Set<String>, Set<String>> solveSystem(int nbVars, int[][] equations) {
      Model naive = new Model();
      BoolVar[] naiveVars = naive.boolVarArray(nbVars);
      for (int[] equation : equations) {
         IntVar[] elements = new IntVar[equation.length];
         for (int i = 0; i < equation.length; i++) {
            elements[i] = naiveVars[equation[i]];
         }
         naive.sum(elements, "!=", 1).post();
      }

      Model gXor = new Model();
      BoolVar[] gXorVars = gXor.boolVarArray(nbVars);
      BoolVar[][] gXorEquations = new BoolVar[equations.length][];
      for (int i = 0; i < equations.length; i++) {
         gXorEquations[i] = new BoolVar[equations[i].length];
         for (int j = 0; j < equations[i].length; j++) {
            gXorEquations[i][j] = gXorVars[equations[i][j]];
         }
      }
      gXor.post(new Constraint("GlobalXor", new AbstractXORPropagator(gXorVars, gXorEquations, new FullInferenceEngine(), new FullRulesApplier(), gXor.getSolver())));

      return new Pair<>(collectSolutions(naive, naiveVars), collectSolutions(gXor, gXorVars));
   }

}
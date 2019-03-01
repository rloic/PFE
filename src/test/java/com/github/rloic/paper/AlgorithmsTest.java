package com.github.rloic.paper;

import com.github.rloic.NaiveSolution;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class AlgorithmsTest {

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
      int len = D + 1;
      Model m = new Model();
      BoolVar[] vars = m.boolVarArray(len);
      m.post(new Constraint("GlobalC", new GlobalXorPropagator(vars, new BoolVar[][]{
            new BoolVar[]{vars[A], vars[C], vars[D]},
            new BoolVar[]{vars[B], vars[C], vars[D]}
      })));

      Set<String> solutions = collectSolutions(m, vars);
      Set<String> expected = NaiveSolution.solve(new int[len], new int[][]{
            new int[]{A, C, D},
            new int[]{B, C, D}
      });
      assertTrue(expected.containsAll(solutions));
   }

   @Test
   void should_pass_3_elements_xor() {
      int len = C + 1;
      Model m = new Model();
      BoolVar[] vars = m.boolVarArray(len);
      m.post(new Constraint("GlobalC", new GlobalXorPropagator(vars, new BoolVar[][]{
            new BoolVar[]{vars[A], vars[B], vars[C]}
      })));

      Set<String> solutions = collectSolutions(m, vars);
      Set<String> expected = NaiveSolution.solve(new int[len], new int[][]{
            new int[]{A, B, C}
      });
      assertTrue(expected.containsAll(solutions));
   }

   @Test
   void should_pass_5_elements_xor() {
      int len = F + 1;
      Model m = new Model();
      BoolVar[] vars = m.boolVarArray(len);
      m.post(new Constraint("GlobalC", new GlobalXorPropagator(vars, new BoolVar[][]{
            new BoolVar[]{vars[A], vars[B], vars[C]},
            new BoolVar[]{vars[D]},
            new BoolVar[]{vars[E], vars[F]},
      })));

      Set<String> solutions = collectSolutions(m, vars);
      Set<String> expected = NaiveSolution.solve(new int[len], new int[][]{
            new int[]{A, B, C},
            new int[]{D},
            new int[]{E, F}
      });
      assertTrue(expected.containsAll(solutions));
   }

   @Test
   void should_pass_test4() {
      int len = E + 1;
      Model m = new Model();
      BoolVar[] vars = m.boolVarArray(len);
      m.post(new Constraint("GlobalC", new GlobalXorPropagator(vars, new BoolVar[][]{
            new BoolVar[]{vars[A], vars[D], vars[C]},
            new BoolVar[]{vars[B], vars[C], vars[E]},
      })));

      Set<String> solutions = collectSolutions(m, vars);
      Set<String> expected = NaiveSolution.solve(new int[len], new int[][]{
            new int[]{A, C, D},
            new int[]{B, C, E}
      });
      assertTrue(expected.containsAll(solutions));
   }

   @Test
   void should_pass_test5() {
      int len = G + 1;
      Model m = new Model();
      BoolVar[] vars = m.boolVarArray(len);
      m.post(new Constraint("GlobalC", new GlobalXorPropagator(vars, new BoolVar[][]{
            new BoolVar[]{vars[A], vars[B], vars[C]},
            new BoolVar[]{vars[C], vars[D], vars[E]},
            new BoolVar[]{vars[E], vars[F], vars[G]}
      })));

      Set<String> solutions = collectSolutions(m, vars);
      Set<String> expected = NaiveSolution.solve(new int[len], new int[][]{
            new int[]{A, B, C},
            new int[]{C, D, E},
            new int[]{E, F, G}
      });
      assertTrue(expected.containsAll(solutions));
   }

   @Test
   void should_pass_test6() {
      int len = F + 1;
      Model m = new Model();
      BoolVar[] vars = m.boolVarArray(len);
      m.post(new Constraint("GlobalC", new GlobalXorPropagator(vars, new BoolVar[][]{
            new BoolVar[]{vars[A], vars[C], vars[E], vars[F]},
            new BoolVar[]{vars[B], vars[C], vars[E], vars[F]},
            new BoolVar[]{vars[D], vars[E], vars[F]}
      })));

      Set<String> solutions = collectSolutions(m, vars);
      Set<String> expected = NaiveSolution.solve(new int[len], new int[][]{
            new int[]{A, C, E, F},
            new int[]{B, C, E, F},
            new int[]{D, E, F}
      });
      assertTrue(expected.containsAll(solutions));
   }

   private Set<String> collectSolutions(Model m, BoolVar[] vars) {
      Solver s = m.getSolver();
      Set<String> solutions = new HashSet<>();
      while (s.solve()) {
         solutions.add(Arrays.stream(vars).map(IntVar::getValue).collect(Collectors.toList()).toString());
      }
      return solutions;
   }

}
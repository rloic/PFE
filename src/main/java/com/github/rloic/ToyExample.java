package com.github.rloic;

import com.github.rloic.paper.impl.dancinglinks.Affectation;
import com.github.rloic.paper.impl.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.impl.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.impl.dancinglinks.actions.UpdaterState;
import com.github.rloic.paper.impl.dancinglinks.actions.impl.Algorithms;
import com.github.rloic.paper.impl.dancinglinks.dancinglinks.DancingLinksMatrix;
import com.github.rloic.xorconstraint.BasePropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;


public class ToyExample {

   static Scanner s = new Scanner(System.in);
   static IDancingLinksMatrix matrix;
   static Stack<IUpdater> updates = new Stack<>();

   static String readLine() {
      return s.nextLine();
   }

   public static void main(String[] args) throws ContradictionException {
      final int A = 0;
      final int B = 1;
      final int C = 2;
      final int D = 3;
      final int E = 4;
      final int F = 5;
      final int G = 6;
      final int H = 7;

      Model m = new Model();
      BoolVar[] variables = m.boolVarArray(4);
      Solver s = m.getSolver();
      m.sum(new IntVar[]{variables[B], variables[D]}, "!=", 1).post();
      m.post(
            new Constraint(
                  "GlobalXOR",
                  new BasePropagator(
                        variables,
                        new BoolVar[][]{
                              new BoolVar[]{variables[A], variables[C], variables[D]},
                              new BoolVar[]{variables[B], variables[C], variables[D]},
                        },
                        s
                  )
            )
      );

      while (s.solve()) {
         System.out.println(Arrays.toString(variables));
         s.printShortStatistics();
      }
      s.printShortStatistics();
   }

   static boolean play(int variable, boolean value) {
      List<Affectation> affectations = new ArrayList<>();
      IUpdater assignment;
      if (value) {
         assignment = Algorithms.buildTrueAssignation(variable);
      } else {
         assignment = Algorithms.buildFalseAssignation(variable);
      }
      UpdaterState state = assignment.update(matrix, affectations);
      System.out.println(variable + "<-" + value + ": " + state);
      System.out.println("Inferences => " + affectations);
      if (state != UpdaterState.EARLY_FAIL) {
         updates.add(assignment);
      }
      System.out.println(matrix);
      return state == UpdaterState.DONE;
   }

   static void rollback() {
      updates.pop().restore(matrix);
      System.out.println(matrix);
   }

}

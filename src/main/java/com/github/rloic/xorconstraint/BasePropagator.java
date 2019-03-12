package com.github.rloic.xorconstraint;

import com.github.rloic.paper.impl.dancinglinks.Affectation;
import com.github.rloic.paper.impl.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.impl.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.impl.dancinglinks.actions.UpdaterState;
import com.github.rloic.paper.impl.dancinglinks.actions.impl.Algorithms;
import com.github.rloic.paper.impl.dancinglinks.dancinglinks.Cell;
import com.github.rloic.paper.impl.dancinglinks.dancinglinks.DancingLinksMatrix;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static com.github.rloic.paper.impl.dancinglinks.actions.UpdaterState.DONE;

public class BasePropagator extends Propagator<BoolVar> {

   private long backTrackCount = 0;
   private final Solver solver;
   private final Stack<IUpdater> commands;
   private final IDancingLinksMatrix matrix;

   public BasePropagator(
         BoolVar[] vars,
         BoolVar[][] xors,
         Solver solver
   ) {
      super(vars, PropagatorPriority.CUBIC, true);
      this.solver = solver;
      this.commands = new Stack<>();

      final Map<BoolVar, Integer> indexOf = new HashMap<>();
      int lastIndex = 0;
      for (BoolVar variable : vars) {
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

      matrix = new DancingLinksMatrix(equations, lastIndex);
   }

   private boolean backTrack() {
      if (backTrackCount < solver.getBackTrackCount()) {
         backTrackCount = solver.getBackTrackCount();
         return true;
      }
      return false;
   }

   private void doBackTrack() {
      int step = 0;
      while (commands.size() >= solver.getCurrentDepth()) {
         commands.pop().restore(matrix);
        // debug(null, "Pop(" + (step++) + ")");
      }
   }

   protected IUpdater onPropagate(int variable, boolean value) {
      if (value) {
         return Algorithms.buildTrueAssignation(variable);
      } else {
         return Algorithms.buildFalseAssignation(variable);
      }
   }

   @Override
   public void propagate(int evtmask) {
      Algorithms.gauss(matrix);
   }

   @Override
   public void propagate(int idxVarInProp, int mask) throws ContradictionException {
      if (backTrack()) {
         debug(idxVarInProp, "\n\n\nbefore backtrack");
         while (!matrix.isUndefined(idxVarInProp)) {
            commands.pop().restore(matrix);
         }
         debug(idxVarInProp, "\n\n\nafter backtrack");
      }

      if (!matrix.isUndefined(idxVarInProp)) {
         if((matrix.isTrue(idxVarInProp) && vars[idxVarInProp].getValue() == 0) || (matrix.isFalse(idxVarInProp) && vars[idxVarInProp].getValue() == 1)) {
            throw new ContradictionException();
         }
         return;
      }

      List<Affectation> inferences = new ArrayList<>();
      Affectation _affectation = new Affectation(idxVarInProp, vars[idxVarInProp].getValue() == 1);
      IUpdater updater = onPropagate(idxVarInProp, vars[idxVarInProp].getValue() == 1);
      UpdaterState state = updater.update(matrix, inferences);

      switch (state) {
         case DONE:
            commands.add(updater);
            break;
         case EARLY_FAIL:
            throw new ContradictionException();
         case LATE_FAIL:
            updater.restore(matrix);
            throw new ContradictionException();
      }

      for (int i = 0; i < inferences.size(); i++) {
         Affectation inference = inferences.get(i);
         int variable = inference.variable;
         boolean value = inference.value;

         if (!matrix.isUndefined(variable)) {
            if ((matrix.isTrue(variable) && !value) || (matrix.isFalse(variable) && value)) {
               throw new IllegalStateException();
            }
         } else {
            updater = onPropagate(inference.variable(), inference.value());
            if (updater.update(matrix, inferences) != DONE) throw new IllegalStateException();
            commands.add(updater);
         }
      }
     // debug(idxVarInProp, null);
      for (Affectation affectation : inferences) {
         affectation.propagate(vars, this);
      }
   }

   @Override
   public ESat isEntailed() {
     // debug(null, "call isEntailed()");
      for (int equation = 0; equation < matrix.nbEquations(); equation++) {
         int nbTrue = 0;
         for(Cell.Data variableCells : matrix.variablesOf(equation)) {
            boolean isTrue;
            if(matrix.isUndefined(variableCells.variable)) {
               assert vars[variableCells.variable].isInstantiated();
               isTrue = vars[variableCells.variable].getValue() == 1;
            } else {
               assert (vars[variableCells.variable].getValue() == 1) == (matrix.isTrue(variableCells.variable));
               isTrue = matrix.isTrue(variableCells.variable);
            }
            if (isTrue) {
               nbTrue += 1;
            }
         }
         if(nbTrue == 1) {
            return ESat.FALSE;
         }
      }
      return ESat.TRUE;
   }

   void debug(Integer n, String message) {
      StringBuilder str;
      if (message != null) {
         str = new StringBuilder(message);
      } else {
         str = new StringBuilder();
      }
      str.append("           ");
      for (int i = 0; i < vars.length; i++) {
         if(n != null && i == n) {
            str.append("   v");
         } else {
            str.append("    ");
         }
      }
      /*
      str.append('\n');
      str.append("           ");
      for (int i = 0; i < vars.length; i++) {
         str.append(padLeft(String.valueOf(i), 4));
      }
      */
      str.append('\n');
      str.append(" choco     ");
      for (int i = 0; i < vars.length; i++) {
         if (vars[i].isInstantiated()) {
            if (vars[i].getValue() == 1) {
               str.append("   1");
            } else {
               str.append("   0");
            }
         } else {
            str.append("    ");
         }
      }
      str.append('\n');
      str.append(" internal  ");
      for (int i = 0; i < vars.length; i++) {
         if (!matrix.isUndefined(i)) {
            if (matrix.isTrue(i)) {
               str.append("   1");
            } else {
               str.append("   0");
            }
         } else {
            str.append("    ");
         }
      }
      str.append("\n\n");
      try (FileWriter log = new FileWriter(new File("log.txt"), true)) {
         log.write(str.toString());
      } catch (IOException e) {}


   }

   public static String padLeft(String s, int n) {
      return String.format("%" + n + "s", s);
   }

}

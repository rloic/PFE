package com.github.rloic.xorconstraint;

import com.github.rloic.paper.impl.dancinglinks.Affectation;
import com.github.rloic.paper.impl.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.impl.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.impl.dancinglinks.actions.UpdaterList;
import com.github.rloic.paper.impl.dancinglinks.actions.UpdaterState;
import com.github.rloic.paper.impl.dancinglinks.actions.impl.Algorithms;
import com.github.rloic.paper.impl.dancinglinks.dancinglinks.DancingLinksMatrix;
import com.github.rloic.paper.impl.dancinglinks.dancinglinks.cell.Data;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.util.ESat;

import java.util.*;

import static com.github.rloic.paper.impl.dancinglinks.actions.UpdaterState.DONE;

public class BasePropagator extends Propagator<BoolVar> {

   private final Solver solver;
   private final Stack<UpdaterList> commands;
   private final IDancingLinksMatrix matrix;
   private long backTrackCount = 0L;
   private long currentDepth = 0L;

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
      currentDepth = solver.getCurrentDepth() - 1;
      while (commands.size() > currentDepth) {
         commands.pop().restore(matrix);
      }
   }

   private boolean goDeeper() {
      if (currentDepth < solver.getCurrentDepth()) {
         currentDepth = solver.getCurrentDepth();
         return true;
      }
      return false;
   }

   private void createStep() {
      commands.add(new UpdaterList());
   }

   private IUpdater onPropagate(int variable, boolean value) {
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
      if (backTrack()) doBackTrack();
      if (goDeeper()) createStep();

      if (!matrix.isUndefined(idxVarInProp)) {
         if(
               (matrix.isTrue(idxVarInProp) && vars[idxVarInProp].getValue() == 0)
                     || (matrix.isFalse(idxVarInProp) && vars[idxVarInProp].getValue() == 1)
         ) {
            throw new ContradictionException();
         }
         return;
      }

      UpdaterList step = commands.peek();
      List<Affectation> inferences = new ArrayList<>();
      Affectation _affectation = new Affectation(idxVarInProp, vars[idxVarInProp].getValue() == 1);
      IUpdater updater = onPropagate(idxVarInProp, vars[idxVarInProp].getValue() == 1);
      UpdaterState state = updater.update(matrix, inferences);

      switch (state) {
         case DONE:
            step.addCommitted(updater);
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
            step.addCommitted(updater);
         }
      }

      for (Affectation affectation : inferences) {
         affectation.propagate(vars, this);
      }
   }

   @Override
   public ESat isEntailed() {
      for (int equation = 0; equation < matrix.nbEquations(); equation++) {
         int nbTrue = 0;
         for(Data variableCells : matrix.variablesOf(equation)) {
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

}

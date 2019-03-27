package com.github.rloic.xorconstraint;

import com.github.rloic.paper.dancinglinks.InferenceEngine;
import com.github.rloic.paper.dancinglinks.actions.*;
import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.Algorithms;
import com.github.rloic.paper.dancinglinks.cell.Cell;
import com.github.rloic.paper.dancinglinks.cell.Column;
import com.github.rloic.paper.dancinglinks.cell.Row;
import com.github.rloic.paper.dancinglinks.impl.DancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.cell.Data;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.*;

import static com.github.rloic.paper.dancinglinks.actions.UpdaterState.DONE;

public class BasePropagator extends Propagator<BoolVar> {

   private final Solver solver;
   private final Stack<UpdaterList> commands;
   private final IDancingLinksMatrix matrix;
   private long backTrackCount = 0L;
   private long currentDepth = 0L;

   private int arity;

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
      arity = lastIndex;
      matrix = new DancingLinksMatrix(equations, lastIndex);
   }


   @Override
   public int arity() {
      return matrix.numberOfUndefinedVariables();
   }

   @Override
   public void propagate(int evtmask) {
      Algorithms.gauss(matrix);
      List<Affectation> inferences = new ArrayList<>();
      for (Row equation : matrix.activeEquations()) {
         inferences.addAll(InferenceEngine.infer(matrix, equation.index));
      }

      IUpdater updater;
      for (int i = 0; i < inferences.size(); i++) {
         Affectation inference = inferences.get(i);
         int variable = inference.variable;
         boolean value = inference.value;

         if (matrix.isUndefined(variable)) {
            updater = onPropagate(inference.variable(), inference.value());
            UpdaterState status = updater.update(matrix, inferences);
            assert status == DONE;
         }
         assert (matrix.isTrue(variable) && value) || (matrix.isFalse(variable) && !value);
      }

      assert checkState(matrix);

      for (Affectation affectation : inferences) {
         try {
            affectation.propagate(vars, this);
         } catch (ContradictionException contradiction) {
            throw new RuntimeException(contradiction);
         }
      }

   }

   @Override
   public void propagate(int idxVarInProp, int mask) throws ContradictionException {
      if (backTrack()) doBackTrack();
      if (goDeeper()) createSteps();

      if (!matrix.isUndefined(idxVarInProp)) {
         if (
               (matrix.isTrue(idxVarInProp) && isFalse(vars[idxVarInProp]))
                     || (matrix.isFalse(idxVarInProp) && isTrue(vars[idxVarInProp]))
         ) {
            throw new ContradictionException();
         }
         return;
      }

      UpdaterList step = commands.peek();
      List<Affectation> inferences = new ArrayList<>();
      Affectation _affectation = new Affectation(idxVarInProp, isTrue(vars[idxVarInProp]));
      IUpdater updater = onPropagate(idxVarInProp, isTrue(vars[idxVarInProp]));
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

         if (matrix.isUndefined(variable)) {
            updater = onPropagate(inference.variable(), inference.value());
            UpdaterState status = updater.update(matrix, inferences);
            assert status == DONE;
            step.addCommitted(updater);
         }
         assert (matrix.isTrue(variable) && value) || (matrix.isFalse(variable) && !value);
      }

      assert checkState(matrix);

      for (Affectation affectation : inferences) {
         affectation.propagate(vars, this);
      }
   }

   @Override
   public ESat isEntailed() {
      boolean hasUndefined = false;
      for (int equation = 0; equation < matrix.nbEquations(); equation++) {
         int nbTrue = 0;
         for (Data variableCells : matrix.variablesOf(equation)) {
            boolean isTrue;

            if (matrix.isUndefined(variableCells.variable)) {
               hasUndefined = true;
               isTrue = isTrue(vars[variableCells.variable]);
            } else {
               assert isTrue(vars[variableCells.variable]) == (matrix.isTrue(variableCells.variable));
               isTrue = matrix.isTrue(variableCells.variable);
            }
            if (isTrue) {
               nbTrue += 1;
            }
         }
         if (nbTrue == 1) {
            return ESat.FALSE;
         }
      }
      return hasUndefined ? ESat.UNDEFINED : ESat.TRUE;
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

   private void createSteps() {
      while (commands.size() < currentDepth - 1) {
         commands.add(Nothing.INSTANCE);
      }
      commands.add(new UpdaterList());
   }

   private IUpdater onPropagate(int variable, boolean value) {
      if (value) {
         return Algorithms.buildTrueAssignation(variable);
      } else {
         return Algorithms.buildFalseAssignation(variable);
      }
   }

   private boolean isTrue(BoolVar variable) {
      return variable.isInstantiated() && variable.getValue() == 1;
   }

   private boolean isFalse(BoolVar variable) {
      return variable.isInstantiated() && variable.getValue() == 0;
   }

   private boolean checkState(IDancingLinksMatrix m) {
      return atLeastTwoVarsPerLine(m)
            && twoVarsAndOneAtTrueImpliesOtherAtTrue(m)
            && basesEqualities(m)
            && isNormalForm(m);
   }

   private boolean atLeastTwoVarsPerLine(IDancingLinksMatrix m) {
      for (Row equation : m.activeEquations()) {
         int count = 0;
         for (Data variable : equation) {
            count += 1;
         }
         if (count < 2) {
            return false;
         }
      }
      return true;
   }

   private boolean twoVarsAndOneAtTrueImpliesOtherAtTrue(IDancingLinksMatrix m) {
      for (Row equation : m.activeEquations()) {
         int count = 0;
         int nbTrues = 0;
         for (Data variable : equation) {
            count += 1;
            if (m.isTrue(variable.variable)) {
               nbTrues += 1;
            }
         }
         if (count == 2 && nbTrues == 1) {
            return false;
         }
      }
      return true;
   }

   private boolean basesEqualities(IDancingLinksMatrix m) {
      for (Row equation : m.activeEquations()) {
         int baseVar = m.baseVariableOf(equation);
         if (baseVar != -1 && m.isTrue(baseVar)) {
            for (Row equationJ : m.activeEquations()) {
               if (equation != equationJ && m.sameOffBaseVariables(equation, equationJ)) {
                  if (!m.isTrue(m.baseVariableOf(equationJ))) {
                     return false;
                  }
               }
            }
         }
      }
      return true;
   }

   private boolean isNormalForm(IDancingLinksMatrix m) {
      for (Row equation : m.activeEquations()) {
         int baseVar = m.baseVariableOf(equation);
         if (baseVar != -1) {
            int count = 0;
            for (Data it : m.equationsOf(baseVar)) {
               count += 1;
            }
            if (count != 1) {
               return false;
            }
         }
      }
      return true;
   }


}

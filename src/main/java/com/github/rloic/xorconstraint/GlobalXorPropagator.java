package com.github.rloic.xorconstraint;

import com.github.rloic.inference.IAffectation;
import com.github.rloic.inference.impl.TrueAffectation;
import com.github.rloic.paper.Algorithms;
import com.github.rloic.paper.XORMatrix;
import com.github.rloic.paper.impl.AdjacencyMatrixImpl;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.constraints.PropagatorPriority;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.events.IntEventType;
import org.chocosolver.util.ESat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GlobalXorPropagator extends Propagator<BoolVar> {

   private final int[][] equations;
   private XORMatrix matrix;

   private long lastBackTrack = 0L;
   private int nbCall = 0;
   private final Solver solver;

   public GlobalXorPropagator(BoolVar[] variables, BoolVar[][] xors, Solver solver) {
      super(variables, PropagatorPriority.CUBIC, true);
      final Map<BoolVar, Integer> indexOf = new HashMap<>();
      int lastIndex = 0;
      for (BoolVar variable : variables) {
         indexOf.put(variable, lastIndex++);
      }
      equations = new int[xors.length][];
      for (int i = 0; i < xors.length; i++) {
         final int length = xors[i].length;
         equations[i] = new int[length];
         for (int j = 0; j < length; j++) {
            equations[i][j] = indexOf.get(xors[i][j]);
         }
      }
      matrix = new AdjacencyMatrixImpl(equations, vars.length);
      this.solver = solver;
   }

   @Override
   public int getPropagationConditions(int vIdx) {
      return IntEventType.all();
   }

   private boolean isTrue(int idxVarInProp) {
      return vars[idxVarInProp].getValue() == 1;
   }

   private boolean isFalse(int idxVarInProp) {
      return vars[idxVarInProp].getValue() == 0;
   }

   private boolean chocoHasBacktrack() {
      boolean hasBackTrack = lastBackTrack < solver.getBackTrackCount();
      lastBackTrack = solver.getBackTrackCount();
      return hasBackTrack;
   }

   private boolean chocoMakeReassignment(int idxVarInProp) {
      return (matrix.isFixed(idxVarInProp) && isTrue(idxVarInProp) != matrix.isTrue(idxVarInProp));
   }

   private void infers(XORMatrix matrix, List<IAffectation> affectations) throws ContradictionException {
      for(int i = 0; i < affectations.size(); i++) {
         IAffectation affectation = affectations.get(i);
         if (!matrix.isFixed(affectation.variable())) {
            affectation.apply(matrix, affectations);
         }
      }
   }

   @Override
   public void propagate(int idxVarInProp, int mask) throws ContradictionException {
      Logger.trace("Nb call " + (nbCall++));
      List<IAffectation> affectations = new ArrayList<>();
      if (chocoHasBacktrack() || chocoMakeReassignment(idxVarInProp)) {
         if (!hardReset(matrix, affectations)) {
            throw new ContradictionException();
         }
      } else if(isTrue(idxVarInProp)) {
         if(!Algorithms.assignToTrue(matrix, idxVarInProp, affectations)) {
            throw new ContradictionException();
         }
      } else if (isFalse(idxVarInProp) ) {
         if(!Algorithms.assignToFalse(matrix, idxVarInProp, affectations)) {
            throw new ContradictionException();
         }
      }
      infers(matrix, affectations);

      for(IAffectation affectation : affectations) {
         affectation.propagate(vars, this);
      }
   }

   @Override
   public void propagate(int evtmask) {
      hardReset(matrix, new ArrayList<>());
   }


   @Override
   public ESat isEntailed() {
      if (!hardReset(matrix, new ArrayList<>())) return ESat.FALSE;
      for (int equation : matrix.equations()) {
         if (matrix.nbUnknowns(equation) != 0) {
            return ESat.UNDEFINED;
         }
      }
      assert matrix.stableState();
      return ESat.TRUE;
   }

   private boolean hardReset(XORMatrix matrix, List<IAffectation> affectations) {
      matrix.clear();
      for (int j = 0; j < vars.length; j++) {
         if (vars[j].isInstantiated()) {
            matrix.fix(j, vars[j].getValue() == 1);
         }
      }
      return Algorithms.normalize(matrix, affectations);
   }

}

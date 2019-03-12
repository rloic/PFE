package com.github.rloic.inference.impl;

import com.github.rloic.inference.IAffectation;
import com.github.rloic.paper.Algorithms;
import com.github.rloic.paper.XORMatrix;
import org.chocosolver.solver.exception.ContradictionException;

import java.util.List;

public class FalseAffectation extends Affectation {

   private int[][] reify;

   public FalseAffectation(int variable, int[][] reify) {
      super(variable, false);
      this.reify = reify;
   }

   @Override
   public void apply(XORMatrix matrix, List<IAffectation> queue) throws ContradictionException {
      assert matrix.stableState();
      assert !matrix.isFixed(variable);
      matrix.fix(variable, false);
      if (matrix.isBase(variable)) {
         int pivot = matrix.pivotOf(variable);
         matrix.removeFromBase(variable);
         if (matrix.isEmptyEquation(pivot)) {
            matrix.removeRow(pivot);
         } else {
            assert Algorithms.makePivot(matrix, reify, pivot, matrix.firstEligibleBase(pivot), queue);
         }
      } else {
         for(int equation : matrix.equationsOf(variable)) {
            if (matrix.isInvalid(equation)) raiseConstradiction();
            if (matrix.nbUnknowns(equation) == 1) {
               if (matrix.nbTrues(equation) == 1) {
                  queue.add(new TrueAffectation(matrix.firstUnknown(equation)));
               } else if (matrix.nbTrues(equation) == 0) {
                  queue.add(new FalseAffectation(matrix.firstUnknown(equation), reify));
               }
            }
         }
      }
      matrix.removeVar(variable);
      assert matrix.stableState();
   }

   @Override
   public void unapply(XORMatrix matrix) {
      // TODO
   }

}

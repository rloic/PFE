package com.github.rloic.paper.dancinglinks.actions.impl;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Propagation;
import com.github.rloic.paper.dancinglinks.actions.Updater;

import java.util.List;

/**
 * Remove the equation from the matrix
 * preCondition:
 */
public class RemoveEquation extends Updater implements IUpdater {

   private final int equation;

   public RemoveEquation(int equation) {
      this.equation = equation;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      return matrix.isEmpty(equation) || matrix.nbUnknowns(equation) == 0;
      // return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences) {
      matrix.removeEquation(equation);
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      matrix.restoreEquation(equation);
   }

   @Override
   public String toString() {
      return "RemoveEquation(equation=" + equation + ")";
   }
}

package com.github.rloic.paper.dancinglinks.actions.impl;

import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Updater;

import java.util.List;

public class RemoveEquation extends Updater implements IUpdater {

   private final int equation;

   public RemoveEquation(int equation) {
      this.equation = equation;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      assert matrix.isEmpty(equation);
      return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Affectation> inferences) {
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

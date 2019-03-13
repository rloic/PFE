package com.github.rloic.paper.dancinglinks.actions.impl;

import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Updater;

import java.util.List;

public class RemoveVariable extends Updater implements IUpdater {

   private final int variable;

   public RemoveVariable(int variable) {
      this.variable = variable;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      assert matrix.isUnused(variable);
      return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Affectation> inferences) {
      matrix.removeVariable(variable);
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      matrix.restoreVariable(variable);
   }

   @Override
   public String toString() {
      return "RemoveVariable(variable=" + variable + ")";
   }
}

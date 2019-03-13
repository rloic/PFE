package com.github.rloic.paper.impl.dancinglinks.actions.impl;

import com.github.rloic.paper.impl.dancinglinks.Affectation;
import com.github.rloic.paper.impl.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.impl.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.impl.dancinglinks.actions.Updater;
import com.github.rloic.paper.impl.dancinglinks.dancinglinks.cell.Data;

import java.util.List;

public class Fix extends Updater implements IUpdater {

   private final int variable;
   private final boolean value;

   public Fix(int variable, boolean value) {
      this.variable = variable;
      this.value = value;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      assert matrix.isUndefined(variable);
      return true;
   }

   @Override
   protected boolean postCondition(IDancingLinksMatrix matrix) {
      for(Data it : matrix.equationsOf(variable)) {
         if(matrix.isInvalid(it.equation)) return false;
      }
      assert !matrix.isUndefined(variable);
      return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Affectation> inferences) {
      matrix.set(variable, value);
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      matrix.unSet(variable);
   }

   @Override
   public String toString() {
      return "Fix(variable=" + variable + ", value=" + value + ")";
   }
}

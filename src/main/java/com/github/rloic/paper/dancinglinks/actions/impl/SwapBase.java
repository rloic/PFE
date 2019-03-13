package com.github.rloic.paper.dancinglinks.actions.impl;

import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Updater;

import java.util.List;

public class SwapBase extends Updater implements IUpdater {

   private final int oldBaseVar;
   private final int newBaseVar;

   public SwapBase(int oldBaseVar, int newBaseVar) {
      this.oldBaseVar = oldBaseVar;
      this.newBaseVar = newBaseVar;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      assert matrix.isBase(oldBaseVar) && !matrix.isBase(newBaseVar);
      return true;
   }

   @Override
   protected boolean postCondition(IDancingLinksMatrix matrix) {
      return matrix.isBase(newBaseVar) && !matrix.isBase(oldBaseVar);
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Affectation> inferences) {
      int pivot = matrix.pivotOf(oldBaseVar);
      matrix.setBase(pivot, newBaseVar);
      matrix.setOffBase(oldBaseVar);
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      int pivot = matrix.pivotOf(newBaseVar);
      matrix.setBase(pivot, oldBaseVar);
      matrix.setOffBase(newBaseVar);
   }

   @Override
   public String toString() {
      return "SwapBase(oldBaseVar=" + oldBaseVar + ", newBaseVar=" + newBaseVar + ")";
   }
}

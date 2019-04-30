package com.github.rloic.paper.dancinglinks.actions.impl;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Propagation;
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
      assert  matrix.isBase(newBaseVar) && !matrix.isBase(oldBaseVar);
      return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences) {
      int pivot = matrix.pivotOf(oldBaseVar);
      matrix.setOffBase(oldBaseVar);
      matrix.setBase(pivot, newBaseVar);
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      int pivot = matrix.pivotOf(newBaseVar);
      matrix.setOffBase(newBaseVar);
      matrix.setBase(pivot, oldBaseVar);
   }

   @Override
   public String toString() {
      return "SwapBase(oldBaseVar=" + oldBaseVar + ", newBaseVar=" + newBaseVar + ")";
   }
}

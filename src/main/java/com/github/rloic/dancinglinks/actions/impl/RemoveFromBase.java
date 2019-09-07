package com.github.rloic.dancinglinks.actions.impl;

import com.github.rloic.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.dancinglinks.actions.IUpdater;
import com.github.rloic.dancinglinks.actions.Propagation;
import com.github.rloic.dancinglinks.actions.Updater;

import java.util.List;

/**
 * Remove a variable from the list of bases of the matrix
 */
public class RemoveFromBase extends Updater implements IUpdater {

   private final int base;
   private final int pivot;

   public RemoveFromBase(int pivot, int base) {
      this.base = base;
      this.pivot = pivot;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      assert matrix.isBase(base)
            && matrix.pivotOf(base) == pivot
            && matrix.baseVariableOf(pivot) == base;
      return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences) {
      matrix.setOffBase(base);
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      matrix.setBase(pivot, base);
   }
}

package com.github.rloic.dancinglinks.actions.impl;

import com.github.rloic.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.dancinglinks.actions.IUpdater;
import com.github.rloic.dancinglinks.actions.Propagation;
import com.github.rloic.dancinglinks.actions.Updater;

import java.util.List;

/**
 * Transform the matrix M into M' such as M[
 */
public class XOR extends Updater implements IUpdater {

   private final int target;
   private final int pivot;

   public XOR(int target, int pivot) {
      this.target = target;
      this.pivot = pivot;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      return matrix.isValid(target);
   }

   @Override
   protected boolean postCondition(IDancingLinksMatrix matrix) {
      return matrix.isValid(target);
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences) {
      matrix.xor(target, pivot);
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      matrix.xor(target, pivot);
   }

   @Override
   public String toString() {
      return "XOR(target=" + target + ", pivot=" + pivot + ")";
   }
}

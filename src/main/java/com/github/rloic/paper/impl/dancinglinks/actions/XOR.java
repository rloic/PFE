package com.github.rloic.paper.impl.dancinglinks.actions;

import com.github.rloic.paper.impl.dancinglinks.DancingLinksMatrix;
import com.github.rloic.transaction.TransactionException;
import com.github.rloic.transaction.Transactionnal;

public class XOR extends Transactionnal {

   private final DancingLinksMatrix m;
   private final int target;
   private final int pivot;

   public XOR(DancingLinksMatrix m, int target, int pivot) {
      this.m = m;
      this.target = target;
      this.pivot = pivot;
   }

   @Override
   public void rollback() {
      m.xor(target, pivot);
   }

   @Override
   protected void onCommit() throws TransactionException {
      m.xor(target, pivot);
      if (m.isInvalid(target)) restoreAndFail();
   }
}

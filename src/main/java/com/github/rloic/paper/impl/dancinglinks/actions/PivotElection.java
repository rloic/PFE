package com.github.rloic.paper.impl.dancinglinks.actions;

import com.github.rloic.paper.impl.dancinglinks.DancingLinksMatrix;
import com.github.rloic.transaction.TransactionException;
import com.github.rloic.transaction.Transactionnal;

public class PivotElection extends Transactionnal {

   private final DancingLinksMatrix matrix;
   private final int variable;
   private final int pivot;

   public PivotElection(DancingLinksMatrix matrix, int variable, int pivot) {
      this.matrix = matrix;
      this.variable = variable;
      this.pivot = pivot;
   }

   @Override
   public void rollback() {
      matrix.removeFromBase(variable);
   }

   @Override
   protected void onCommit() throws TransactionException {
      if (matrix.isNone(pivot, variable) || matrix.isFalse(pivot, variable)) fail();
      matrix.addToBase(pivot, variable);
   }
}

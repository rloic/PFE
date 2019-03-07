package com.github.rloic.transaction;

import java.util.List;

public class Transaction extends Transactionnal {

   private final List<Transactionnal> steps;
   private int lastCommitted;

   public Transaction(List<Transactionnal> steps) {
      this.steps = steps;
      lastCommitted = 0;
   }

   @Override
   public void rollback() {
      while (lastCommitted > 0) {
         lastCommitted -= 1;
         steps.get(lastCommitted).rollback();
      }
   }

   @Override
   protected void onCommit() throws TransactionException {
      while (lastCommitted < steps.size() && steps.get(lastCommitted).commit()) {
         lastCommitted += 1;
      }

      if (lastCommitted != steps.size()) {
         restoreAndFail();
      }
   }
}

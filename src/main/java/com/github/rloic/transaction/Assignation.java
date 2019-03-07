package com.github.rloic.transaction;

import com.github.rloic.paper.XORMatrix;

public class Assignation extends Transactionnal {

   private final XORMatrix m;
   private final int variable;
   private final boolean value;

   public Assignation(XORMatrix m, int variable, boolean value) {
      this.m = m;
      this.variable = variable;
      this.value = value;
   }

   @Override
   public void rollback() {
      m.unfix(variable);
   }

   @Override
   protected void onCommit() throws TransactionException {
      if (m.isFixed(variable)) {
         if ((m.isTrue(variable) && value) || (m.isFalse(variable) && !value)) return;
         fail();
      }
      int assignment = value ? 1 : 0;
      for(int equation : m.equationsOf(variable)) {
         if (m.nbUnknowns(equation) == 1 && m.nbTrues(equation) + assignment == 1 ) fail();
      }
      m.fix(variable, value);
   }
}

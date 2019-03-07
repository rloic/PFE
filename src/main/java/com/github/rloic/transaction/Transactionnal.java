package com.github.rloic.transaction;

public abstract class Transactionnal {

   final public boolean commit() {
      try {
         onCommit();
         return true;
      } catch (TransactionException te) {
         if (te == TransactionException.RESTORE_AND_FAIL) {
            rollback();
         }
         return false;
      }
   }

   abstract public void rollback();

   abstract protected void onCommit() throws TransactionException;

   final protected void fail() throws TransactionException {
      throw TransactionException.FAIL;
   }

   final protected void restoreAndFail() throws TransactionException {
      throw TransactionException.RESTORE_AND_FAIL;
   }

}

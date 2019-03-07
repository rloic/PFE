package com.github.rloic.transaction;

public class TransactionException extends Exception {

   public static TransactionException FAIL = new TransactionException();
   public static TransactionException RESTORE_AND_FAIL = new TransactionException();

   private TransactionException() {}

}

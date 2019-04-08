package com.github.rloic.common.collections;

public class IterableExtensions {

   public static int len(Iterable iterable) {
      int len = 0;
      for (Object ignored : iterable) {
         len += 1;
      }
      return len;
   }

}

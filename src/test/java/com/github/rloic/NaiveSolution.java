package com.github.rloic;

import java.util.*;

public class NaiveSolution {

   public static Set<String> solve(int[] vars, int[][] equations) {
      Set<String> result = new HashSet<>();
      for (int i = 0; i <= 1; i++) {
         vars[0] = i;
         result.addAll(solve(vars, equations, 1));
      }
      return result;
   }

   private static Set<String> solve(int[] vars, int[][] equations, int offset) {
      Set<String> result = new HashSet<>();
      if (offset == vars.length) {
         if (satisfy(vars, equations)) {
            result.add(Arrays.toString(vars));
         } else {
            return Collections.emptySet();
         }
      } else {
         for (int i = 0; i <= 1; i++) {
            vars[offset] = i;
            result.addAll(solve(vars, equations, offset + 1));
         }
      }
      return result;
   }

   private static boolean satisfy(int[] vars, int[][] equations) {
      for (int[] equation : equations) {
         int sum = 0;
         for (int i : equation) {
            sum += vars[i];
         }
         if (sum == 1) return false;
      }
      return true;
   }

}

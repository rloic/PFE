package com.github.rloic;

import com.github.rloic.aes.EnumFilter;
import com.github.rloic.aes.GlobalXOR;
import com.github.rloic.aes.KeyBits;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;

public class AppDancing {

   public static void main(String[] args) {

      final int rounds = 3;
      final int objStep1 = 1;
      final KeyBits AES_VERSION = AES_256;

      GlobalXOR gXor = new GlobalXOR(rounds, objStep1, AES_VERSION);
      Solver s = gXor.m.getSolver();
      EnumFilter enumFilter = new EnumFilter(gXor.m, gXor.sBoxes, objStep1);
      s.plugMonitor(enumFilter);
      s.setSearch(
            Search.intVarSearch(gXor.sBoxes),
            Search.intVarSearch(gXor.m.retrieveBoolVars()),
            Search.intVarSearch(gXor.m.retrieveIntVars(false))
      );
      while (s.solve()) {
         print(gXor.sBoxes);
         s.printShortStatistics();
      }
      s.printShortStatistics();
   }

   private static void print(BoolVar[] variables) {
      for(BoolVar variable : variables) {
         if(variable.isInstantiated()) {
            if (variable.getValue() == 1) {
               System.out.print('1');
            } else {
               System.out.print('0');
            }
         } else {
            System.out.print('x');
         }
         System.out.print(' ');
      }
      System.out.println();
   }

}

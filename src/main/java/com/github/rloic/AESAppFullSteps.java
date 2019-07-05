package com.github.rloic;

import com.github.rloic.aes.AESFullSteps;
import com.github.rloic.strategy.WDeg;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.variables.BoolVar;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;

public class AESAppFullSteps {
   public static void main(String[] args) {
      AESFullSteps model = new AESFullSteps(AES_128, 3, 5);

      Solver s = model.m.getSolver();

      s.setSearch(
            new WDeg(model.sBoxes, 0L, new IntDomainMin(), model.constraintsOf),
            new WDeg(concat(model.ΔX, model.ΔSX, model.ΔY, model.ΔZ), 0L, new IntDomainMin(), model.constraintsOf),
            Search.intVarSearch(model.m.retrieveIntVars(true))
      );
      s.setSearch(
            Search.lastConflict(s.getSearch())
      );

      while (s.solve()) {
         s.printShortStatistics();
      }
   }

   private static BoolVar[] concat(BoolVar[]... args) {
      int length = 0;
      for (BoolVar[] arg : args) {
         length += arg.length;
      }
      BoolVar[] result = new BoolVar[length];
      int cpt = 0;
      for (BoolVar[] arg: args) {
         for (BoolVar element : arg) {
            result[cpt++] = element;
         }
      }
      return result;
   }
}

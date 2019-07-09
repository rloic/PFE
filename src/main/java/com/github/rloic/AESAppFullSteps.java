package com.github.rloic;

import com.github.rloic.aes.AESFullSteps;
import com.github.rloic.aes.KeyBits;
import com.github.rloic.strategy.WDeg;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.variables.BoolVar;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;

public class AESAppFullSteps {
   public static void main(String[] args) {

      KeyBits version = AES_128;
      if (args.length >= 1) {
         if (args[0].equalsIgnoreCase("AES-192")) {
            version = AES_192;
         } else if (args[0].equalsIgnoreCase("AES-256")) {
            version = AES_256;
         } else if (!args[0].equalsIgnoreCase("AES-128")) {
            throw new RuntimeException("Invalid version" + args[0]);
         }
      }

      int rounds = 3;
      if (args.length >= 2) {
         rounds = Integer.parseInt(args[1]);
      }

      int nbSboxes = 5;
      if (args.length >= 3) {
         nbSboxes = Integer.parseInt(args[2]);
      }

      AESFullSteps model = new AESFullSteps(version, rounds, nbSboxes);
      Solver s = model.m.getSolver();

      BoolVar[] abstractVars = concat(
            model.ΔX,
            model.ΔSX,
            model.ΔY,
            model.ΔZ,
            model.ΔK
      );

      s.setSearch(
            Search.intVarSearch(model.nbActives),
            new WDeg(model.sBoxes, 0L, new IntDomainMin(), model.constraintsOf),
            new WDeg(abstractVars, 0L, new IntDomainMin(), model.constraintsOf),
            Search.intVarSearch(model.m.retrieveIntVars(true))
      );
      s.setSearch(
            Search.lastConflict(s.getSearch())
      );

      Solution bestSolution = s.findOptimalSolution(model.objective, Model.MINIMIZE);
      if (bestSolution != null) {
         s.printShortStatistics();
      } else {
         throw new RuntimeException("Not solution found");
      }
   }

   private static BoolVar[] concat(BoolVar[]... args) {
      int length = 0;
      for (BoolVar[] arg : args) {
         length += arg.length;
      }
      BoolVar[] result = new BoolVar[length];
      int cpt = 0;
      for (BoolVar[] arg : args) {
         for (BoolVar element : arg) {
            result[cpt++] = element;
         }
      }
      return result;
   }
}

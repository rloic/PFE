package com.github.rloic;

import com.github.rloic.midori.MidoriGlobalXOR;
import com.github.rloic.midori.MidoriSumXOR;
import com.github.rloic.midori.MidoriGlobalSum;
import com.github.rloic.util.Logger;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.search.strategy.selectors.values.IntDomainMin;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.rloic.util.Logger.InfoLogger.INFO;

public class MidoriEx {

   public static void main(String[] args) {
      Logger.level(INFO);



      int r=3;
      int objStep1=3;
      MidoriSumXOR model = new MidoriSumXOR(r, objStep1);
      benchModel(model.m, model.sBoxes, model.assignedVar);
      System.out.println("\n\n");

      MidoriGlobalXOR globalXOR = new MidoriGlobalXOR(r, objStep1);
      benchModel(globalXOR.m, globalXOR.sBoxes, globalXOR.assignedVar);
      System.out.println("\n\n");

      MidoriGlobalSum sumAndGlobal = new MidoriGlobalSum(r, objStep1);
      benchModel(sumAndGlobal.m, sumAndGlobal.sBoxes, sumAndGlobal.assignedVar);
   }

   private static void benchModel(Model model, BoolVar[] sBoxes, BoolVar[] assignedVar) {
      long start = System.currentTimeMillis();
      Solver solver = model.getSolver();
      solver.setSearch(
            new CustomDomOverWDeg(sBoxes, 0L, new IntDomainMin())
           // new CustomDomOverWDeg(assignedVar, 0L, new IntDomainMin())
      );
      while (solver.solve()) {
        // solver.printShortStatistics();
        // printSBoxes(sBoxes);
      }

      solver.printShortStatistics();
      long end = System.currentTimeMillis();
      Logger.info("CPU Time: " + (end - start) + " ms");
   }


   private static void benchModel(Model model, BoolVar[] sBoxes, int objStep1) {
      long start = System.currentTimeMillis();
      Solver solver = model.getSolver();
      //solver.plugMonitor(new EnumFilter(model, sBoxes, objStep1));
      //solver.setSearch(
      //      new CustomDomOverWDeg(sBoxes, 0L, new IntDomainMin())
      //);
      while (solver.solve()) {
        // solver.printShortStatistics();
        // printSBoxes(sBoxes);
      }

      solver.printShortStatistics();
      long end = System.currentTimeMillis();
      Logger.info("CPU Time: " + (end - start) + " ms");
   }


   private static void printSBoxes(BoolVar[] sBoxes) {
      List<Integer> values = Arrays.stream(sBoxes)
            .map(IntVar::getValue)
            .collect(Collectors.toList());
      System.out.println("Solution: " + values);
   }

}

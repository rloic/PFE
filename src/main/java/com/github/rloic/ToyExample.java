package com.github.rloic;

import com.github.rloic.aes.AdvancedModelPaper;
import com.github.rloic.aes.EnumFilter;
import com.github.rloic.aes.GlobalXOR;
import com.github.rloic.aes.KeyBits;
import com.github.rloic.benchmark.Experiment;
import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.UpdaterState;
import com.github.rloic.paper.dancinglinks.Algorithms;
import com.github.rloic.wip.AdvancedModelPaperWithGlobalXOR;
import com.github.rloic.xorconstraint.BasePropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.strategy.Search;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;


public class ToyExample {

   static Scanner s = new Scanner(System.in);
   static IDancingLinksMatrix matrix;
   static Stack<IUpdater> updates = new Stack<>();

   static String readLine() {
      return s.nextLine();
   }

   public static void main(String[] args) throws ContradictionException {
      List<Experiment> experiments = Arrays.asList(
            new Experiment(3, 5, AES_128, 0, TimeUnit.SECONDS),
            new Experiment(3, 5, AES_128, 0, TimeUnit.SECONDS),
            new Experiment(3, 5, AES_128, 0, TimeUnit.SECONDS)
      );

      for(Experiment experiment : experiments) {
         System.out.println("******************************************");
         System.out.println(experiment);
         GlobalXOR model = new GlobalXOR(experiment.round, experiment.objStep1, experiment.key);
         Solver solver = model.m.getSolver();
         solver.plugMonitor(new EnumFilter(model.m, model.sBoxes, experiment.objStep1));
         solver.setSearch(
               Search.intVarSearch(model.sBoxes),
               Search.intVarSearch(model.assignedVar),
               Search.intVarSearch(model.m.retrieveBoolVars()),
               Search.intVarSearch(model.m.retrieveIntVars(false))
         );
         while (solver.solve()) {
            printVars(model.sBoxes);
            solver.printShortStatistics();
         }
         solver.printShortStatistics();
      }
   }

   static void printVars(BoolVar[] vars) {

      for (BoolVar variable : vars) {
         if (variable.isInstantiated()) {
            if (variable.getValue() == 1) {
               System.out.print("1 ");
            } else {
               System.out.print("0 ");
            }
         } else {
            System.out.print("x ");
         }
      }
      System.out.println();

   }


}

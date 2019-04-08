package com.github.rloic.wip;

import org.chocosolver.solver.variables.BoolVar;

import java.util.function.Predicate;

public class WeightedConstraint {

   private final BoolVar[] vars;
   private final Predicate<BoolVar[]> isViolated;
   public int weight;

   public WeightedConstraint(
         BoolVar[] vars,
         Predicate<BoolVar[]> isViolated
   ) {
      this.vars = vars;
      this.isViolated = isViolated;
      this.weight = 0;
   }

   public boolean isViolated() {
      return arity() == 0 && isViolated.test(vars);
   }

   public int arity() {
      int arity = 0;
      for (BoolVar var : vars) {
         if (!var.isInstantiated()) {
            arity += 1;
         }
      }
      return arity;
   }

}

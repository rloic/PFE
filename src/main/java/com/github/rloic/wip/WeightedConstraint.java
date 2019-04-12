package com.github.rloic.wip;

import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.ESat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

public class WeightedConstraint<V extends Variable> {

   private final V[] vars;
   private final Predicate<V[]> isViolated;
   public int weight;

   public WeightedConstraint(
         V[] vars,
         Predicate<V[]> isViolated
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
      for (V var : vars) {
         if (!var.isInstantiated()) {
            arity += 1;
         }
      }
      return arity;
   }

   public static WeightedConstraint<Variable> wrap(Constraint constraint) {

      List<Variable> list = new ArrayList<>();
      for(Propagator propagator : constraint.getPropagators()) {
         for(Variable var : propagator.getVars()) {
            if (!list.contains(var)) {
               list.add(var);
            }
         }
      }

      Variable[] variables = new Variable[list.size()];
      list.toArray(variables);

      return new WeightedConstraint<>(variables, ignored ->
            constraint.isSatisfied() == ESat.FALSE
      );

   }

   public static List<WeightedConstraint<Variable>> wrapAsMany(Constraint constraint) {

      List<WeightedConstraint<Variable>> weightedConstraints = new ArrayList<>();
      for(Propagator propagator : constraint.getPropagators()) {
         weightedConstraints.add(new WeightedConstraint<>(propagator.getVars(), ignored -> propagator.isEntailed() == ESat.FALSE));
      }
      return weightedConstraints;

   }


}

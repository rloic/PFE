package com.github.rloic.strategy;

import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Sequential extends AbstractStrategy<IntVar> {

   private final IntValueSelector valueSelector;

   public Sequential(
         IntVar[] variables,
         IntValueSelector valueSelector
   ) {
      super(variables);
      this.valueSelector = valueSelector;
   }

   @Override
   public Decision<IntVar> getDecision() {
      List<IntVar> unknowns = Arrays.stream(vars)
            .filter(it -> !it.isInstantiated())
            .collect(Collectors.toList());

      if (unknowns.isEmpty()) return null;

      IntVar best = unknowns.get(0);
      int currentVal = valueSelector.selectValue(best);
      return best.getModel()
            .getSolver()
            .getDecisionPath().makeIntDecision(best, DecisionOperatorFactory.makeIntEq(), currentVal);
   }

}

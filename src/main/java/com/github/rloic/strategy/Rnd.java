package com.github.rloic.strategy;

import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;

import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class Rnd extends AbstractStrategy<IntVar> {

   private final Random random;
   private final IntValueSelector valueSelector;

   public Rnd(
         IntVar[] variables,
         long seed,
         IntValueSelector valueSelector
   ) {
      super(variables);
      this.random = new Random(seed);
      this.valueSelector = valueSelector;
   }

   @Override
   public Decision<IntVar> getDecision() {
      List<IntVar> unknowns = Arrays.stream(vars)
            .filter(it -> !it.isInstantiated())
            .collect(Collectors.toList());

      if (unknowns.isEmpty()) return null;

      IntVar best = unknowns.get(random.nextInt(unknowns.size()));
      int currentVal = valueSelector.selectValue(best);
      return best.getModel()
            .getSolver()
            .getDecisionPath().makeIntDecision(best, DecisionOperatorFactory.makeIntEq(), currentVal);
   }
}

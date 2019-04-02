package com.github.rloic.strategy;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.xorconstraint.BasePropagator;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.stream.Collectors;

public class DDeg extends AbstractStrategy<IntVar> {

   private final IntValueSelector valueSelector;
   private final Map<BoolVar, Integer> indexOf;
   private final IDancingLinksMatrix matrix;

   public DDeg(
         IntVar[] variables,
         IntValueSelector valueSelector,
         BasePropagator propagator
   ) {
      super(variables);
      this.valueSelector = valueSelector;
      this.indexOf = propagator.indexOf;
      this.matrix = propagator.matrix;
   }

   @Override
   public Decision<IntVar> getDecision() {
      List<IntVar> unknowns = Arrays.stream(vars)
            .filter(it -> !it.isInstantiated())
            .collect(Collectors.toList());

      if (unknowns.isEmpty()) return null;
      unknowns.sort((lhs, rhs) -> {
         Integer idxA = indexOf.get(lhs);
         Integer idxB = indexOf.get(rhs);

         int equationsOfA = (idxA != null) ? matrix.numberOfEquationsOf(idxA) : 0;
         int equationsOfB = (idxB != null) ? matrix.numberOfEquationsOf(idxB) : 0;
         return Integer.compare(equationsOfB, equationsOfA);
      });

      IntVar best = unknowns.get(0);
      int currentVal = valueSelector.selectValue(best);
      return best.getModel()
            .getSolver()
            .getDecisionPath().makeIntDecision(best, DecisionOperatorFactory.makeIntEq(), currentVal);

   }
}

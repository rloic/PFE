package com.github.rloic.strategy;

import com.github.rloic.wip.WeightedConstraint;
import gnu.trove.list.array.TIntArrayList;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.memory.IStateInt;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.search.loop.monitors.IMonitorContradiction;
import org.chocosolver.solver.search.strategy.assignments.DecisionOperatorFactory;
import org.chocosolver.solver.search.strategy.decision.Decision;
import org.chocosolver.solver.search.strategy.selectors.values.IntValueSelector;
import org.chocosolver.solver.search.strategy.strategy.AbstractStrategy;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.objects.IntMap;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class WDeg extends AbstractStrategy<IntVar> implements IMonitorContradiction {

   /**
    * Temporary. Stores index of variables with the same (best) score
    */
   private TIntArrayList bests;

   /**
    * Randomness to break ties
    */
   private java.util.Random random;

   /**
    * The way value is selected for a given variable
    */
   private IntValueSelector valueSelector;

   /***
    * Pointer to the last uninstantiated variable
    */
   private IStateInt last;

   private final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   /**
    * Creates a DomOverWDeg variable selector
    *
    * @param variables     decision variables
    * @param seed          seed for breaking ties randomly
    * @param valueSelector a value selector
    */
   public WDeg(
         IntVar[] variables,
         long seed,
         IntValueSelector valueSelector,
         Int2ObjectMap<List<WeightedConstraint>> constraintsOf
   ) {
      super(variables);
      Model model = variables[0].getModel();
      bests = new TIntArrayList();
      this.valueSelector = valueSelector;
      random = new java.util.Random(seed);
      this.last = model.getEnvironment().makeInt(vars.length - 1);
      this.constraintsOf = constraintsOf;
   }

   @Override
   public boolean init() {
      Solver solver = vars[0].getModel().getSolver();
      if(!solver.getSearchMonitors().contains(this)) {
         vars[0].getModel().getSolver().plugMonitor(this);
      }
      return true;
   }

   @Override
   public void remove() {
      Solver solver = vars[0].getModel().getSolver();
      if(solver.getSearchMonitors().contains(this)) {
         vars[0].getModel().getSolver().unplugMonitor(this);
      }
   }

   @Override
   public void onContradiction(ContradictionException cex) {
      if (cex.c instanceof Propagator) {
         if (cex.v != null) {
            List<WeightedConstraint> constraints = constraintsOf.get(cex.v.getId());
            for(WeightedConstraint constraint : constraints) {
               if (constraint.isViolated()) {
                  constraint.weight += 1;
               }
            }
         }
      }
   }


   @Override
   public Decision<IntVar> computeDecision(IntVar variable) {
      if (variable == null || variable.isInstantiated()) {
         return null;
      }
      int currentVal = valueSelector.selectValue(variable);
      return variable.getModel().getSolver().getDecisionPath().makeIntDecision(variable, DecisionOperatorFactory.makeIntEq(), currentVal);
   }

   @Override
   public Decision<IntVar> getDecision() {
      IntVar best = null;
      bests.resetQuick();
      long _d1 = Integer.MAX_VALUE;
      long _d2 = 0;
      int to = last.get();
      for (int idx = 0; idx <= to; idx++) {
         int dsize = vars[idx].getDomainSize();
         if (dsize > 1) {
            int weight = weight(vars[idx]);
            long c1 = dsize * _d2;
            long c2 = _d1 * weight;
            if (c1 < c2) {
               bests.resetQuick();
               bests.add(idx);
               _d1 = dsize;
               _d2 = weight;
            } else if (c1 == c2) {
               bests.add(idx);
            }
         } else {
            // swap
            IntVar tmp = vars[to];
            vars[to] = vars[idx];
            vars[idx] = tmp;
            idx--;
            to--;
         }
      }
      last.set(to);
      if (bests.size() > 0) {
         int currentVar = bests.get(random.nextInt(bests.size()));
         best = vars[currentVar];
      }
      return computeDecision(best);
   }

   private int weight(IntVar v) {
      int w = 1;
      List<WeightedConstraint> constraints = constraintsOf.get(v.getId());
      if (constraints == null || constraints.isEmpty()) return w;
      for (WeightedConstraint constraint : constraints) {
         if (constraint.arity() > 1) {
            w += constraint.weight;
         }
      }
      return w;
   }
}

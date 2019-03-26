package com.github.rloic;

import gnu.trove.list.array.TIntArrayList;

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
import org.chocosolver.util.objects.IntMap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Implementation of CustomDomOverWDeg[1].
 *
 * [1]: F. Boussemart, F. Hemery, C. Lecoutre, and L. Sais, Boosting Systematic Search by Weighting
 * Constraints, ECAI-04. <br/>
 *
 * @author Charles Prud'homme
 * @since 12/07/12
 */
public class CustomDomOverWDeg extends AbstractStrategy<IntVar> implements IMonitorContradiction {

   private FileWriter writer;

   /**
    * Kind of duplicate of pid2ari to limit calls of backtrackable objects
    */
   private Map<Integer, ExpirableInteger> pid2arity;

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
   /**
    * Map (propagator - weight), where weight is the number of times the propagator fails.
    */
   protected IntMap p2w;

   /**
    * Creates a DomOverWDeg variable selector
    *
    * @param variables     decision variables
    * @param seed          seed for breaking ties randomly
    * @param valueSelector a value selector
    */
   public CustomDomOverWDeg(
         IntVar[] variables,
         long seed,
         IntValueSelector valueSelector
   ) {
      super(variables);
      Model model = variables[0].getModel();
      pid2arity = new HashMap<>(model.getCstrs().length * 3 / 2 + 1);
      bests = new TIntArrayList();
      this.valueSelector = valueSelector;
      random = new java.util.Random(seed);
      this.last = model.getEnvironment().makeInt(vars.length - 1);
      p2w = new IntMap(10, -1);
      this.writer = writer;
      init(Stream.of(model.getCstrs())
            .flatMap(c -> Stream.of(c.getPropagators()))
            .toArray(Propagator[]::new));
   }

   private void init(Propagator[] propagators) {
      for (Propagator propagator : propagators) {
         p2w.put(propagator.getId(), 0);
      }
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
         Propagator p = (Propagator) cex.c;
         p2w.putOrAdjust(p.getId(), 1, 1);
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
      pid2arity.clear();
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
      int nbp = v.getNbProps();
      for (int i = 0; i < nbp; i++) {
         Propagator prop = v.getPropagator(i);
         int pid = prop.getId();
         // if the propagator has been already evaluated
         //ExpirableInteger arity = pid2arity.get(pid);
         //if (pid2arity.get(pid) != null && arity.value() != null) {
         //   w += p2w.get(prop.getId());
         //} else {
            // the arity of this propagator is not yet known
            int futVars = prop.arity();
            assert futVars > -1;
            pid2arity.put(pid, new ExpirableInteger(vars.length * 10, futVars));
            if (futVars > 1) {
               w += p2w.get(prop.getId());
            }
         //}
      }
      return w;
   }

   static class ExpirableInteger {

      long maxUsage;
      long usage = 0L;
      private int value;

      ExpirableInteger(long maxUsage, int value) {
         this.maxUsage = System.currentTimeMillis() + maxUsage;
         this.value = value;
      }

      Integer value() {
         if (usage == maxUsage) return null;
         usage += 1;
         return value;
      }

   }

}

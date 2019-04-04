package com.github.rloic.strategy;

import com.github.rloic.xorconstraint.BasePropagator;
import gnu.trove.list.array.TIntArrayList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
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
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.chocosolver.util.objects.IntMap;

import java.util.List;
import java.util.Random;
import java.util.Set;


public class WDeg extends AbstractStrategy<IntVar> implements IMonitorContradiction {

   private IntValueSelector valueSelector;
   private int[] scores;
   private IStateInt last;
   private Random random;
   private TIntArrayList bests;
   private final IntMap indexOf;

   public WDeg(
         IntVar[] variables,
         long seed,
         IntValueSelector valueSelector,
         Model model,
         BasePropagator propagator
   ) {
      super(variables);
      this.valueSelector = valueSelector;
      this.scores = new int[variables.length];
      this.last = model.getEnvironment().makeInt(vars.length - 1);
      this.random = new Random(seed);
      this.bests = new TIntArrayList();
      this.indexOf = new IntMap(10, -1);
      for (int i = 0; i < variables.length; i++) {
         indexOf.put(variables[i].getId(), i);
      }
      for (Variable var : variables) {
         int varI = indexOf.get(var.getId());
         if (varI != -1) {
            scores[varI] += var.getNbProps();
         }
      }
   }

   @Override
   public boolean init() {
      Solver solver = vars[0].getModel().getSolver();
      if (!solver.getSearchMonitors().contains(this)) {
         vars[0].getModel().getSolver().plugMonitor(this);
      }
      return true;
   }

   @Override
   public void remove() {
      Solver solver = vars[0].getModel().getSolver();
      if (solver.getSearchMonitors().contains(this)) {
         vars[0].getModel().getSolver().unplugMonitor(this);
      }
   }

   private int indexOf(Variable variable) {
      return indexOf.get(variable.getId());
   }

   @Override
   public void onContradiction(ContradictionException cex) {
      if (cex.c instanceof Propagator) {
         Propagator propagator = (Propagator) cex.c;

         if (propagator instanceof BasePropagator) {
            BasePropagator baseP = (BasePropagator) propagator;
            IntList viewed = new IntArrayList();
            for (Set<BoolVar> column : baseP.columns) {
               incrementAllElements(cex, viewed, column);
            }
            /*viewed.clear();
            for (Set<BoolVar> row : baseP.rows) {
               incrementAllElements(cex, viewed, row);
            }*/
            viewed.clear();
            for (Set<BoolVar> round: baseP.rounds) {
               incrementAllElements(cex, viewed, round);
            }
         }
         for (Variable var : propagator.getVars()) {
            int varIndex = indexOf(var);
            if (varIndex != -1) {
               scores[varIndex] += 1000;
            }
         }
      }
   }

   private void incrementAllElements(ContradictionException cex, IntList viewed, Set<BoolVar> cluster) {
      if (cluster.contains(cex.v)) {
         for (BoolVar var : cluster) {
            int varIndex = indexOf(var);
            if (varIndex != -1 && !viewed.contains(varIndex)) {
               scores[varIndex] += 1;
               viewed.add(varIndex);
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
      long maxWeight = 0;
      int to = last.get();
      for (int idx = 0; idx <= to; idx++) {
         if (vars[idx].getDomainSize() > 1) {
            int weight = weight(vars[idx]);
            if (weight > maxWeight) {
               bests.resetQuick();
               bests.add(idx);
               maxWeight = weight;
            } else if (weight == maxWeight) {
               bests.add(idx);
            }
         } else {
            // swap
            IntVar tmp = vars[to];
            vars[to] = vars[idx];
            vars[idx] = tmp;
            indexOf.put(vars[to].getId(), to);
            indexOf.put(vars[idx].getId(), idx);
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
      return scores[indexOf(v)];
   }

}

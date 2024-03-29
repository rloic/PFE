package com.github.rloic.strategy;

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

import java.util.Random;
import java.util.stream.Stream;


public class CustomDomOverWDeg extends AbstractStrategy<IntVar> implements IMonitorContradiction {

   private TIntArrayList bests;

   private Random random;

   private IntValueSelector valueSelector;

   private IStateInt last;

   private IntMap p2w;

   private double pBefore;

   public CustomDomOverWDeg(
         IntVar[] variables,
         long seed,
         IntValueSelector valueSelector
   ) {
      super(variables);
      Model model = variables[0].getModel();
      bests = new TIntArrayList();
      this.valueSelector = valueSelector;
      random = new Random(seed);
      this.last = model.getEnvironment().makeInt(vars.length - 1);
      p2w = new IntMap(10, -1);
      pBefore = domainsSize();
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
            idx--;
            to--;
         }
      }
      last.set(to);
      if (bests.size() > 0) {
         int currentVar = bests.get(random.nextInt(bests.size()));
         best = vars[currentVar];
      }
      //System.err.println(1.0 - domainsSize() / pBefore);
      pBefore = domainsSize();
      return computeDecision(best);
   }

   private int weight(IntVar v) {
      int w = 1;
      int nbp = v.getNbProps();
      for (int i = 0; i < nbp; i++) {
         Propagator prop = v.getPropagator(i);
         int futVars = prop.arity();
         assert futVars > -1;
         if (futVars > 1) {
            w += p2w.get(prop.getId());
         }
      }
      return w;
   }

   double domainsSize() {
      double product = 1.0;
      for (IntVar var : vars) {
         if (var.getDomainSize() == 0) {
            throw new RuntimeException();
         }
         product *= var.getDomainSize();
      }
      return product;
   }

}

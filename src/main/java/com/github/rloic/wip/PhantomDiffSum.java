package com.github.rloic.wip;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

import java.util.Arrays;

public class PhantomDiffSum extends Propagator<IntVar> {

   private final int sum;

   public PhantomDiffSum(int sum, IntVar... vars) {
      super(vars);
      this.sum = sum;
   }

   @Override
   public void propagate(int evtmask) throws ContradictionException {
      if (isEntailed() == ESat.FALSE) throw new ContradictionException().set(this, null, null);
   }

   @Override
   public ESat isEntailed() {
      int _sum = 0;
      for (IntVar var : vars) {
         if (!var.isInstantiated()) return ESat.UNDEFINED;
         _sum += var.getValue();
      }
      return (sum != _sum) ? ESat.TRUE : ESat.FALSE;
   }
}

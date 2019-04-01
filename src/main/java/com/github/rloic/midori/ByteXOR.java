package com.github.rloic.midori;

import org.chocosolver.solver.constraints.Propagator;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.ESat;

public class ByteXOR extends Propagator<IntVar> {

   public ByteXOR(IntVar... vars) {
      super(vars);
   }

   @Override
   public void propagate(int evtmask) throws ContradictionException {

      IntVar notInstantiated = null;
      int xorResult = 0;
      for (IntVar variable : vars) {
         if (variable.isInstantiated()) {
            xorResult = xorResult ^ variable.getValue();
         } else {
            if (notInstantiated != null) return;
            notInstantiated = variable;
         }
      }

      if(notInstantiated == null) {
         if (xorResult != 0) {
            throw new ContradictionException();
         } else {
            return;
         }
      }
      notInstantiated.instantiateTo(xorResult, this);
   }

   @Override
   public ESat isEntailed() {
      int xorResult = 0;
      for (IntVar variable : vars) {
         if (!variable.isInstantiated()) throw new RuntimeException();
         xorResult = xorResult ^ variable.getValue();
      }
      if(xorResult == 0) {
         return ESat.TRUE;
      } else {
         return ESat.FALSE;
      }
   }
}

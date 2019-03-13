package com.github.rloic.inference.impl;

import com.github.rloic.inference.IAffectation;
import com.github.rloic.paper.XORMatrix;
import org.chocosolver.solver.exception.ContradictionException;

import java.util.List;

public class TrueAffectation extends Affectation {

   public TrueAffectation(int variable) {
      super(variable, true);
   }

   @Override
   public void apply(XORMatrix m, List<IAffectation> queue) throws ContradictionException {
      assert m.stableState();
      assert !m.isFixed(variable);
      m.fix(variable, true);
      for (int equation : m.equationsOf(variable)) {
         if (m.isInvalid(equation)) {
            //m.unfix(variable);
            raiseConstradiction();
         }
         if (m.nbUnknowns(equation) == 1 && m.nbTrues(equation) == 1) {
            queue.add(new TrueAffectation(m.firstUnknown(equation)));
         }
      }
      assert m.stableState();
   }

   @Override
   public void unapply(XORMatrix matrix) {
      assert matrix.stableState();
      //matrix.unfix(variable);
      assert matrix.stableState();
   }
}

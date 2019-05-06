package com.github.rloic.midori;

import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.PartialInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.PartialRulesApplier;
import org.chocosolver.solver.variables.IntVar;

/**
 * A Midori model using the globalXor constraint (without Arc Consistency)
 */
final public class MidoriGlobalPartial extends MidoriGlobal {

   public MidoriGlobalPartial(int r, int objStep1) {
      super(r, objStep1);
   }

   public MidoriGlobalPartial(int r, int objStep1, IntVar[] nbActives) {
      super(r, objStep1, nbActives);
   }

   @Override
   protected String getModelName() {
      return "Midori Global[1-3]";
   }

   @Override
   protected InferenceEngine getInferenceEngine() {
      return new PartialInferenceEngine();
   }

   @Override
   protected RulesApplier getRulesApplier() {
      return new PartialRulesApplier();
   }

}
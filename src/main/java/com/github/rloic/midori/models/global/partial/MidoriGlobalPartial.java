package com.github.rloic.midori.models.global.partial;

import com.github.rloic.midori.models.global.MidoriGlobal;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.PartialInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.RulesApplier;
import com.github.rloic.constraints.abstractxor.rulesapplier.impl.PartialRulesApplier;
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
package com.github.rloic.midori;

import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;

final public class MidoriGlobalFull extends MidoriGlobal {

   public MidoriGlobalFull(int r, int objStep1) {
      super(r, objStep1);
   }

   protected String getModelName() {
      return "Midori Global[1-5]";
   }

   protected InferenceEngine getInferenceEngine() {
      return new FullInferenceEngine();
   }

   protected RulesApplier getRulesApplier() {
      return new FullRulesApplier();
   }

}
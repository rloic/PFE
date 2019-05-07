package com.github.rloic.midori.round;

import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.PartialInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.PartialRulesApplier;

public class MidoriGlobalRoundPartial extends MidoriGlobalRound {
    public MidoriGlobalRoundPartial(int r, int objStep1) {
        super(r, objStep1);
    }

    @Override
    protected String getModelName() {
        return "Midori Global Round [1-3]";
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

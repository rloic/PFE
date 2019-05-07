package com.github.rloic.midori.round;

import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;

public class MidoriGlobalRoundFull extends MidoriGlobalRound {

    public MidoriGlobalRoundFull(int r, int objStep1) {
        super(r, objStep1);
    }

    @Override
    protected String getModelName() {
        return "Midori Global Round [1-5]";
    }

    @Override
    protected InferenceEngine getInferenceEngine() {
        return new FullInferenceEngine();
    }

    @Override
    protected RulesApplier getRulesApplier() {
        return new FullRulesApplier();
    }
}

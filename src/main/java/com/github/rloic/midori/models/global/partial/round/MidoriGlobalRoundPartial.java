package com.github.rloic.midori.models.global.partial.round;

import com.github.rloic.midori.models.global.round.MidoriGlobalRound;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.PartialInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.RulesApplier;
import com.github.rloic.constraints.abstractxor.rulesapplier.impl.PartialRulesApplier;

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

package com.github.rloic.midori.models.global.gac.round;

import com.github.rloic.midori.models.global.round.MidoriGlobalRound;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.RulesApplier;
import com.github.rloic.constraints.abstractxor.rulesapplier.impl.FullRulesApplier;

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

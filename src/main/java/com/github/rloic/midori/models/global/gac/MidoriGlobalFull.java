package com.github.rloic.midori.models.global.gac;

import com.github.rloic.midori.models.global.MidoriGlobal;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.RulesApplier;
import com.github.rloic.constraints.abstractxor.rulesapplier.impl.FullRulesApplier;
import org.chocosolver.solver.variables.IntVar;

/**
 * A Midori model using the globalXor constraint (Arc Consistency)
 */
final public class MidoriGlobalFull extends MidoriGlobal {

    public MidoriGlobalFull(int r, int objStep1) {
        super(r, objStep1);
    }

    public MidoriGlobalFull(int r, int objStep1, IntVar[] nbActives) {
        super(r, objStep1, nbActives);
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
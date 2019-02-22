package com.github.rloic.inference;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.Inferences;

public interface InferenceEngine {

    Inferences infer(InferenceMatrix matrix);

    Affectation createAffectation(
            InferenceMatrix matrix,
            int variable,
            boolean value
    );

}

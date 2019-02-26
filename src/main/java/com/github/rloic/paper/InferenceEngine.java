package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.Inferences;

public interface InferenceEngine {

    Inferences applyAndInfer(XORMatrix matrix, Affectation affectation);

}

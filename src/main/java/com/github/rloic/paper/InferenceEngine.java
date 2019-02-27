package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;

import java.util.List;

public interface InferenceEngine {

    List<Affectation> applyAndInfer(XORMatrix matrix, Affectation affectation);

}

package com.github.rloic.inference;

public interface Inference {
    void apply(InferenceMatrix matrix);
    void unapply(InferenceMatrix matrix);
}

package com.github.rloic.inference;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Inferences implements Inference {

    private final List<Inference> inner;

    public Inferences() {
        this.inner = new ArrayList<>();
    }

    public Inferences(List<Inference> inferences) {
        this.inner = new ArrayList<>(inferences);
    }

    public void add(Inference inference) {
        inner.add(inference);
    }
    public void addAll(Collection<Inference> inferences) {
        this.inner.addAll(inferences);
    }
    public void addAll(Inferences inferences) {
        this.inner.addAll(inferences.inner);
    }

    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public void apply(InferenceMatrix matrix) {
        for (Inference inference : inner) {
            inference.apply(matrix);
        }
    }

    @Override
    public void unapply(InferenceMatrix matrix) {
        for (int i = inner.size() - 1; i >= 0; i--) {
            inner.get(i).unapply(matrix);
        }
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}

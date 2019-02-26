package com.github.rloic.inference.impl;

import com.github.rloic.inference.Inference;
import com.github.rloic.inference.InferenceMatrix;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.variables.BoolVar;

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
    public void apply(InferenceMatrix matrix) throws IllegalStateException {
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
    public void constraint(BoolVar[] vars, ICause cause) throws ContradictionException {
        for(Inference inference : inner) {
            inference.constraint(vars, cause);
        }
    }

    @Override
    public String toString() {
        return inner.toString();
    }
}

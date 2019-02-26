package com.github.rloic.inference;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.Inferences;
import org.chocosolver.solver.exception.ContradictionException;

public interface InferenceEngine {

    default Inferences infer(InferenceMatrix matrix) {
        Inferences inferences = inferAndUpdate(matrix);
        inferences.unapply(matrix);
        return inferences;
    }

    Inferences inferAndUpdate(InferenceMatrix matrix);

    Affectation createAffectation(
            InferenceMatrix matrix,
            int variable,
            boolean value
    );

}

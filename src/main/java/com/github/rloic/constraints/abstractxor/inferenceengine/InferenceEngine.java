package com.github.rloic.constraints.abstractxor.inferenceengine;

import com.github.rloic.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.dancinglinks.actions.Propagation;

import java.util.List;

public interface InferenceEngine {

   List<Propagation> infer(
         IDancingLinksMatrix matrix,
         int equation
   );

}

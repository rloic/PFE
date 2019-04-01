package com.github.rloic.paper.dancinglinks.inferenceengine;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.Affectation;

import java.util.List;

public interface InferenceEngine {

   List<Affectation> infer(
         IDancingLinksMatrix matrix,
         int equation
   );

}

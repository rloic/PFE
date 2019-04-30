package com.github.rloic.paper.dancinglinks.inferenceengine;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.actions.Propagation;

import java.util.List;

public interface InferenceEngine {

   List<Propagation> infer(
         IDancingLinksMatrix matrix,
         int equation
   );

}

package com.github.rloic.paper.dancinglinks.inferenceengine.impl;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.actions.Propagation;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;

import java.util.ArrayList;
import java.util.List;

public class FullInferenceEngine implements InferenceEngine {

   public List<Propagation> infer(
         IDancingLinksMatrix matrix,
         int equation
   ) {
      List<Propagation> inferences = new ArrayList<>();
      if(matrix.nbUnknowns(equation) == 1) {
         if (matrix.nbTrues(equation) == 0) {
            inferences.add(new Propagation(matrix.firstUnknown(equation), false));
         } else if (matrix.nbTrues(equation) == 1) {
            inferences.add(new Propagation(matrix.firstUnknown(equation), true));
         }
      }
      return inferences;
   }

}

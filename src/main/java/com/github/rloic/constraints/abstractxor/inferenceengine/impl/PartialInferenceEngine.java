package com.github.rloic.constraints.abstractxor.inferenceengine.impl;

import com.github.rloic.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.dancinglinks.actions.Propagation;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;

import java.util.ArrayList;
import java.util.List;

public class PartialInferenceEngine implements InferenceEngine {

   @Override
   public List<Propagation> infer(
         IDancingLinksMatrix matrix,
         int equation
   ) {
      List<Propagation> inferences = new ArrayList<>();
      if(matrix.nbUnknowns(equation) == 1) {
         if (matrix.nbTrues(equation) == 1) {
            inferences.add(new Propagation(matrix.firstUnknown(equation), true));
         }
      }
      return inferences;
   }

}

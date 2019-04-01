package com.github.rloic.paper.dancinglinks.inferenceengine.impl;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;

import java.util.ArrayList;
import java.util.List;

public class PartialInferenceEngine implements InferenceEngine {

   @Override
   public List<Affectation> infer(
         IDancingLinksMatrix matrix,
         int equation
   ) {
      List<Affectation> inferences = new ArrayList<>();
      if(matrix.nbUnknowns(equation) == 1) {
         if (matrix.nbTrues(equation) == 1) {
            inferences.add(new Affectation(matrix.firstUnknown(equation), true));
         }
      }
      return inferences;
   }

}

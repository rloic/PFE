package com.github.rloic.paper.dancinglinks;

import com.github.rloic.paper.dancinglinks.actions.Affectation;

import java.util.ArrayList;
import java.util.List;

public class InferenceEngine {

   public static List<Affectation> infer(
         IDancingLinksMatrix matrix,
         int equation
   ) {
      List<Affectation> inferences = new ArrayList<>();
      if(matrix.nbUnknowns(equation) == 1) {
         if (matrix.nbTrues(equation) == 0) {
            inferences.add(new Affectation(matrix.firstUnknown(equation), false));
         } else if (matrix.nbTrues(equation) == 1) {
            inferences.add(new Affectation(matrix.firstUnknown(equation), true));
         }
      } else if (matrix.nbUnknowns(equation) == 2) {
         if (matrix.nbTrues(equation) == 0) {
            // TODO
         } else if (matrix.nbTrues(equation) == 1) {
            // TODO
         }
      }
      return inferences;
   }

}

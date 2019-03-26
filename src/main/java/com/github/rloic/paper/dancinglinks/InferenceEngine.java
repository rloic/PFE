package com.github.rloic.paper.dancinglinks;

import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.cell.Data;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

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
      }
      return inferences;
   }

}

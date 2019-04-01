package com.github.rloic.paper.dancinglinks.rulesapplier.impl;

import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.impl.Fix;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.PartialInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;

public class PartialRulesApplier extends FullRulesApplier implements RulesApplier {

   public PartialRulesApplier() {
      super(new PartialInferenceEngine());
   }

   @Override
   public IUpdater buildTrueAssignation(int variable) {
      return new Fix(variable, true)
            .then(matrix -> {
               if (matrix.isBase(variable)) {
                  int pivot = matrix.pivotOf(variable);
                  return inferOnlyForEquation(pivot);
               } else {
                  return inferForAllEquationsOf(matrix, variable);
               }
            });
   }

   @Override
   public IUpdater buildFalseAssignation(int variable) {
      return fix(variable, false)
            .then(matrix -> {
               if (matrix.isBase(variable)) {
                  int pivot = matrix.pivotOf(variable);
                  return inferOnlyForEquation(pivot)
                        .then(
                              matrix.isEmpty(pivot) ?
                                    removeEquation(pivot) :
                                    makePivot(matrix, pivot, variable)
                        );
               } else {
                  return inferForAllEquationsOf(matrix, variable);
               }
            }).then(removeVariable(variable));
   }
}

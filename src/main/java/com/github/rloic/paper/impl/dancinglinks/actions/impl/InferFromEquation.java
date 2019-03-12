package com.github.rloic.paper.impl.dancinglinks.actions.impl;

import com.github.rloic.paper.impl.dancinglinks.Affectation;
import com.github.rloic.paper.impl.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.impl.dancinglinks.InferenceEngine;
import com.github.rloic.paper.impl.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.impl.dancinglinks.actions.Updater;

import java.util.List;

public class InferFromEquation extends Updater implements IUpdater {

   private final int equation;

   public InferFromEquation(int equation) {
      this.equation = equation;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      assert matrix.isValid(equation);
      return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Affectation> inferences) {
      inferences.addAll(InferenceEngine.infer(matrix, equation));
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {}

   @Override
   public String toString() {
      return "InferFromEquation(equation=" + equation + ")";
   }
}

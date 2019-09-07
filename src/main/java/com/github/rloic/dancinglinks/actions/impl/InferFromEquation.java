package com.github.rloic.dancinglinks.actions.impl;

import com.github.rloic.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.dancinglinks.actions.IUpdater;
import com.github.rloic.dancinglinks.actions.Propagation;
import com.github.rloic.dancinglinks.actions.Updater;
import com.github.rloic.constraints.abstractxor.inferenceengine.InferenceEngine;

import java.util.List;

/**
 * Does not update the matrix but add all the inferences that could be made for the equation *equation*
 */
public class InferFromEquation extends Updater implements IUpdater {

   private final int equation;
   private final InferenceEngine engine;

   public InferFromEquation(InferenceEngine engine, int equation) {
      this.engine = engine;
      this.equation = equation;
   }

   @Override
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      assert matrix.isValid(equation);
      return true;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences) {
      inferences.addAll(engine.infer(matrix, equation));
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {}

   @Override
   public String toString() {
      return "InferFromEquation(equation=" + equation + ")";
   }
}

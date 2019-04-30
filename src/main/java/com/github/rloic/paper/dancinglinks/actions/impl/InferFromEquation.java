package com.github.rloic.paper.dancinglinks.actions.impl;

import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.Propagation;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Updater;

import java.util.List;

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

package com.github.rloic.paper.dancinglinks.actions.impl;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.Affectation;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Updater;

import java.util.List;

public class InferAffectation extends Updater implements IUpdater {

   private final int variable;
   private final boolean value;

   public InferAffectation(int variable, boolean value) {
      this.variable = variable;
      this.value = value;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Affectation> inferences) {
      inferences.add(new Affectation(variable, value));
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {

   }
}

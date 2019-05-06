package com.github.rloic.paper.dancinglinks.actions;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;

import java.util.List;

/**
 * The nothing updater.
 * Performs nothing when applied on a matrix
 */
public class Nothing extends UpdaterList implements IUpdater {
   public static final Nothing INSTANCE = new Nothing();

   private Nothing() {
      super("Nothing");
   }

   @Override
   public void addUncommitted(IUpdater updater) {
      throw new RuntimeException();
   }

   @Override
   public void addCommitted(IUpdater updater) {
      throw new RuntimeException();
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences) {}

   @Override
   public void restore(IDancingLinksMatrix matrix) {}
}

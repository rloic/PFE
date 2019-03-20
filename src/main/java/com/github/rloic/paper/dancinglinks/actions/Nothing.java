package com.github.rloic.paper.dancinglinks.actions;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;

import java.util.List;

public class Nothing extends UpdaterList implements IUpdater {

   public static final Nothing INSTANCE = new Nothing();

   private Nothing() {
      super("Nothing");
   }

   @Override
   public void add(IUpdater updater) {
      throw new RuntimeException();
   }

   @Override
   public void addCommitted(IUpdater updater) {
      throw new RuntimeException();
   }

   @Override
   public UpdaterState update(IDancingLinksMatrix matrix, List<Affectation> inferences) { return UpdaterState.DONE; }

   @Override
   public void restore(IDancingLinksMatrix matrix) {}
}

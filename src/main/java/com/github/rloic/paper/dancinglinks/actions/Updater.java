package com.github.rloic.paper.dancinglinks.actions;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;

import java.util.List;

public abstract class Updater implements IUpdater {

   protected boolean preCondition(IDancingLinksMatrix matrix) {
      return true;
   }

   protected boolean postCondition(IDancingLinksMatrix matrix) {
      return true;
   }

   protected abstract void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences);

   @Override
   public UpdaterState update(IDancingLinksMatrix matrix, List<Propagation> inferences) {
      if (!preCondition(matrix)) {
         return UpdaterState.EARLY_FAIL;
      }
      onUpdate(matrix, inferences);
      if (!postCondition(matrix)) {
         return UpdaterState.LATE_FAIL;
      }
      return UpdaterState.DONE;
   }

}

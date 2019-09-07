package com.github.rloic.dancinglinks.actions;

import com.github.rloic.dancinglinks.IDancingLinksMatrix;

import java.util.List;

/**
 * A matrix updater
 */
public abstract class Updater implements IUpdater {

   /**
    * Indicates when the matrix state is illegal (before the update) in regard of the current updater
    * @param matrix The matrix state
    * @return true if the matrix state is illegal else false
    */
   protected boolean preCondition(IDancingLinksMatrix matrix) {
      return true;
   }

   /**
    * Indicates if the matrix state is illegal (after the update) in regard of the current update
    * @param matrix The matrix state
    * @return true if the matrix state is illegal esle false
    */
   protected boolean postCondition(IDancingLinksMatrix matrix) {
      return true;
   }

   /**
    * The updates that must be done on the matrix
    * @param matrix The matrix to update
    * @param inferences The inferences made (updated if the current updater infers new affections)
    */
   protected abstract void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences);

   @Override
   final public UpdaterState update(IDancingLinksMatrix matrix, List<Propagation> inferences) {
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

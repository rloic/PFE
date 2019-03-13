package com.github.rloic.paper.impl.dancinglinks.actions;

import com.github.rloic.paper.impl.dancinglinks.Affectation;
import com.github.rloic.paper.impl.dancinglinks.IDancingLinksMatrix;

import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public interface IUpdater {

   UpdaterState update(IDancingLinksMatrix matrix, List<Affectation> inferences);

   void restore(IDancingLinksMatrix matrix);

   default IUpdater then(Function<IDancingLinksMatrix, IUpdater> updater) {
      final IUpdater self = this;
      return new IUpdater() {
         private IUpdater next = null;

         @Override
         public UpdaterState update(IDancingLinksMatrix matrix, List<Affectation> inferences) {
            UpdaterState state = self.update(matrix, inferences);
            if (state != UpdaterState.DONE) {
               return UpdaterState.LATE_FAIL;
            }
            next = updater.apply(matrix);
            if(next.update(matrix, inferences) != UpdaterState.DONE) {
               return UpdaterState.LATE_FAIL;
            }
            return UpdaterState.DONE;
         }

         @Override
         public void restore(IDancingLinksMatrix matrix) {
            if (next != null) {
               next.restore(matrix);
            }
            self.restore(matrix);
         }

         @Override
         public String toString() {
            return "IUpdater(" + self + ", then=" + next + ")";
         }
      };
   }

   default IUpdater then(Supplier<IUpdater> updater) {
      final IUpdater self = this;
      return new IUpdater() {
         private IUpdater next = null;

         @Override
         public UpdaterState update(IDancingLinksMatrix matrix, List<Affectation> inferences) {
            UpdaterState state = self.update(matrix, inferences);
            if (state != UpdaterState.DONE) {
               return UpdaterState.LATE_FAIL;
            }
            next = updater.get();
            if(next.update(matrix, inferences) != UpdaterState.DONE) {
               return UpdaterState.LATE_FAIL;
            }
            return UpdaterState.DONE;
         }

         @Override
         public void restore(IDancingLinksMatrix matrix) {
            if (next != null) {
               next.restore(matrix);
            }
            self.restore(matrix);
         }

         @Override
         public String toString() {
            return "IUpdater(" + self + ", then=" + next + ")";
         }
      };
   }

   default IUpdater then(IUpdater updater) {
      final IUpdater self = this;
      return new IUpdater() {
         private IUpdater next = null;

         @Override
         public UpdaterState update(IDancingLinksMatrix matrix, List<Affectation> inferences) {
            UpdaterState state = self.update(matrix, inferences);
            if (state != UpdaterState.DONE) {
               return UpdaterState.LATE_FAIL;
            }
            next = updater;
            if(next.update(matrix, inferences) != UpdaterState.DONE) {
               return UpdaterState.LATE_FAIL;
            }
            return UpdaterState.DONE;
         }

         @Override
         public void restore(IDancingLinksMatrix matrix) {
            if (next != null) {
               next.restore(matrix);
            }
            self.restore(matrix);
         }

         @Override
         public String toString() {
            return "IUpdater(" + self + ", then=" + next + ")";
         }
      };
   }

}

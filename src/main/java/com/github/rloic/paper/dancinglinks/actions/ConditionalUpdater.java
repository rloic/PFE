package com.github.rloic.paper.dancinglinks.actions;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;

import java.util.List;
import java.util.function.Predicate;

public class ConditionalUpdater implements IUpdater {

   private boolean leftBranch;
   private final IUpdater left;
   private final IUpdater right;
   private final Predicate<IDancingLinksMatrix> predicate;

   public ConditionalUpdater(Predicate<IDancingLinksMatrix> matrix, IUpdater left, IUpdater right) {
      this.predicate = matrix;
      this.left = left;
      this.right = right;
   }

   @Override
   public UpdaterState update(IDancingLinksMatrix matrix, List<Affectation> inferences) {
      if(predicate.test(matrix)) {
         leftBranch = true;
         return left.update(matrix, inferences);
      } else {
         leftBranch = false;
         return right.update(matrix, inferences);
      }
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      if(leftBranch) {
         left.restore(matrix);
      } else {
         right.restore(matrix);
      }
   }
}

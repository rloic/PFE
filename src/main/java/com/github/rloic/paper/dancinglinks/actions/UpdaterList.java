package com.github.rloic.paper.dancinglinks.actions;


import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;

import java.util.ArrayList;
import java.util.List;

public class UpdaterList extends Updater implements IUpdater {

   private final List<IUpdater> updaters;
   private int lastCommitted = -1;
   private final String name;

   public UpdaterList() {
      this.updaters = new ArrayList<>();
      this.name = "UpdaterList";
   }

   public UpdaterList(String name) {
      this.updaters = new ArrayList<>();
      this.name = name;
   }

   public void add(IUpdater updater) {
      updaters.add(updater);
   }

   public void addCommitted(IUpdater updater) {
      if(lastCommitted != updaters.size() - 1) throw new RuntimeException("Non previous committed updates");
      updaters.add(updater);
      lastCommitted += 1;
   }

   public boolean isEmpty() {
      return updaters.isEmpty();
   }

   public boolean isNotEmpty() {
      return !updaters.isEmpty();
   }

   @Override
   protected boolean postCondition(IDancingLinksMatrix matrix) {
      return lastCommitted == updaters.size() - 1;
   }

   @Override
   protected void onUpdate(IDancingLinksMatrix matrix, List<Propagation> inferences) {
      for (int i = 0; i < updaters.size(); i++) {
         UpdaterState state = updaters.get(i).update(matrix, inferences);
         switch (state) {
            case DONE:
               continue;
            case EARLY_FAIL:
               lastCommitted = i - 1;
               return;
            case LATE_FAIL:
               lastCommitted = i;
               return;
         }
      }
      lastCommitted = updaters.size() - 1;
   }

   @Override
   public void restore(IDancingLinksMatrix matrix) {
      while (lastCommitted != -1) {
         updaters.get(lastCommitted).restore(matrix);
         lastCommitted -= 1;
      }
   }

   @Override
   public String toString() {
      return "UpdaterList(name=" + name + ")";
   }
}

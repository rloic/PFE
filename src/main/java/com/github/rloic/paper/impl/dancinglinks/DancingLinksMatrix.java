package com.github.rloic.paper.impl.dancinglinks;

import com.github.rloic.paper.impl.dancinglinks.actions.PivotElection;
import com.github.rloic.paper.impl.dancinglinks.actions.XOR;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.variables.BoolVar;

import java.util.Arrays;

public class DancingLinksMatrix {

   private static final int NO_PIVOT = -1;

   public final DancingLinks<BoolVar> structure;
   private final boolean[] isBase;
   private final int[] pivotOf;

   public DancingLinksMatrix(BoolVar[] variables, int[][] equations) {
      structure = new DancingLinks<>(variables, equations, it -> {
         if (it.isInstantiated()) {
            if (it.getValue() == 1) {
               return "  1  ";
            } else {
               return "  0  ";
            }
         } else {
            return "  x  ";
         }
      });
      isBase = new boolean[variables.length];
      pivotOf = new int[equations.length];
      Arrays.fill(pivotOf, NO_PIVOT);
   }

   public void addToBase(int pivot, int variable) {
      isBase[variable] = true;
      pivotOf[variable] = pivot;
   }

   public void removeFromBase(int variable) {
      isBase[variable] = false;
      pivotOf[variable] = NO_PIVOT;
   }

   public boolean isUnknown(int equation, int variable) {
      return isUnknown(structure.get(equation, variable));
   }

   public boolean isTrue(int equation, int variable) {
      return isTrue(structure.get(equation, variable));
   }

   public boolean isNone(int equation, int variable) {
      return isNone(structure.get(equation, variable));
   }

   public boolean isFalse(int equation, int variable) {
      return isFalse(structure.get(equation, variable));
   }

   private static boolean isNone(Cell.Data<BoolVar> cell) {
      return !cell.isActive();
   }

   public void xor(int l1, int l2) {
      structure.xor(l1, l2);
   }

   private static boolean isUnknown(Cell.Data<BoolVar> cell) {
      return cell.isActive() && !cell.value.isInstantiated();
   }

   private static boolean isTrue(Cell.Data<BoolVar> cell) {
      return cell.isActive() && cell.value.isInstantiated() && cell.value.getValue() == 1;
   }

   private static boolean isFalse(Cell.Data<BoolVar> cell) {
      return cell.isActive() && cell.value.isInstantiated() && cell.value.getValue() == 0;
   }

   private static boolean hasAOne(DancingLinks<BoolVar> structure, int row, int column) {
      return structure.rowContains(row, it -> isUnknown(it) || isTrue(it) && it.y < column);
   }

   public boolean isInvalid(int l1) {
      Cell<BoolVar> header = structure.row(l1);
      Cell<BoolVar> cell = header.right();
      int unknowns = 0;
      int trues = 0;
      while (cell != header) {
         Cell.Data<BoolVar> data = (Cell.Data<BoolVar>) cell;
         if(isTrue(data)) {
            trues += 1;
         } else if (isUnknown(data)) {
            unknowns += 1;
         }
         cell = cell.right();
      }
      return unknowns != 0 || trues != 1;
   }

}
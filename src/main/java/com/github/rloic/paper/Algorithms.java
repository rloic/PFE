package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.util.Logger;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;
import java.util.function.IntPredicate;

public class Algorithms {

   private static boolean makePivot(XORMatrix m, int pivot, int variable, List<Affectation> F) {
      assert !m.isInvalid(pivot);
      if (m.nbUnknowns(pivot) == 1) {
         assert m.firstUndefined(pivot) != -1;
         if(m.nbTrues(pivot) == 0) {
            F.add(new Affectation(m.firstUndefined(pivot), false));
         } else if (m.nbTrues(pivot) == 1) {
            F.add(new Affectation(m.firstUndefined(pivot), true));
         }
      }
      for (int k : m.rows()) {
         if (k != pivot && (m.isUndefined(k, variable) || m.isTrue(k, variable))) {
            boolean isValid = m.xor(k, pivot);
            if (!isValid) return false;
            assert !m.isInvalid(k);
            if (m.nbUnknowns(k) == 1) {
               if (m.nbTrues(k) == 1) {
                  assert m.firstUndefined(k) != -1;
                  F.add(new Affectation(m.firstUndefined(k), true));
               } else if (m.nbTrues(k) == 0) {
                  F.add(new Affectation(m.firstUndefined(k), false));
               }
            }
         }
      }
      m.setBase(pivot, variable);
      assert m.stableState();
      return true;
   }

   public static boolean normalize(XORMatrix m, List<Affectation> F) {
      if (!m.stableState()) return false;
      removeEmptyColumns(m);
      removeEmptyLines(m);
      if (m.nbColumns() == 0) return true;

      IntIterator validRows = m.rows().iterator();
      int base = validRows.nextInt();
      int nbBases = 0;
      for (int col : m.columns()) {
         if (nbBases == m.nbRows()) break;
         for (int row : m.rows()) {
            if (row >= base && (m.isUndefined(row, col) || m.isTrue(row, col))) {
               if (row != base) m.swap(row, base);
               if (!makePivot(m, base, col, F)) return false;
               nbBases += 1;
               if (validRows.hasNext()) {
                  base = validRows.nextInt();
               } else {
                  break;
               }
            }
         }
      }
      assert m.stableState();
      return true;
   }

   private static void removeEmptyColumns(XORMatrix m) {
      int initialColumns = m.nbColumns();
      for (int col = 0; col < initialColumns; col++) {
         boolean removable = true;
         for (int row : m.rows()) {
            if (m.isUndefined(row, col) || m.isTrue(row, col)) {
               removable = false;
               break;
            }
         }
         if (removable) {
            m.removeVar(col);
         }
      }
   }

   private static void removeEmptyLines(XORMatrix m) {
      int initialRows = m.nbRows();
      for (int row = 0; row < initialRows; row++) {
         boolean removable = true;
         for (int col : m.columns()) {
            if (m.isUndefined(row, col) || m.isTrue(row, col)) {
               removable = false;
            }
         }
         if (removable) {
            m.removeRow(row);
         }
      }
   }

   public static boolean assignToFalse(XORMatrix m, int variable, List<Affectation> queue) {
      if (!m.stableState()) {
         Logger.err("Unstable state\n" + m);
         assert false;
      }
      if (m.isFixed(variable)) {
         return m.isFalse(variable);
      }
      m.fix(variable, false);
      if (m.isBase(variable)) {
         int ivar = m.pivotOf(variable);
         if (m.isInvalid(ivar)) {
            return false;
         }
         m.removeFromBase(variable);
         if (m.emptyRow(ivar)) {
            m.removeRow(ivar);
         } else {
            if(!makePivot(m, ivar, m.firstEligiblePivot(ivar), queue)) {
               return false;
            }
         }
      } else {
         IntList rows = where(m.rows(), row -> m.isFalse(row, variable));
         for (int row : rows) {
            if (m.isInvalid(row)) {
               return false;
            }
            if (m.nbUnknowns(row) == 1) {
               if (m.nbTrues(row) == 1) {
                  queue.add(new Affectation(m.firstUndefined(row), true));
               } else if (m.nbTrues(row) == 0) {
                  queue.add(new Affectation(m.firstUndefined(row), false));
               }
            }
         }
      }
      m.removeVar(variable);
      assert m.stableState();
      return true;
   }

   private static IntList where(IntList elements, IntPredicate predicate) {
      IntList result = new IntArrayList();
      for (int element : elements) {
         if (predicate.test(element)) result.add(element);
      }
      return result;
   }

   public static boolean assignToTrue(XORMatrix m, int variable, List<Affectation> queue) {
      if (!m.stableState()) {
         Logger.err("Unstable state\n" + m);
         assert false;
      }
      if(m.isFixed(variable)) {
         return m.isTrue(variable);
      }
      m.fix(variable, true);
      IntList rows;
      if (m.isBase(variable)) {
         rows = new IntArrayList(new int[]{m.pivotOf(variable)});
      } else {
         rows = where(m.rows(), row -> m.isTrue(row, variable));
      }
      for (int row : rows) {
         if (m.isInvalid(row)) {
            return false;
         }
         if (m.nbUnknowns(row) == 1 && m.nbTrues(row) == 1) {
            queue.add(new Affectation(m.firstUndefined(row), true));
         }
      }
      assert m.stableState();
      return true;
   }

}

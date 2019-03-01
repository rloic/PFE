package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.util.Logger;
import it.unimi.dsi.fastutil.ints.IntIterator;

import java.util.List;

public class Algorithms {

   private static boolean makePivot(XORMatrix m, int pivot, int variable, List<Affectation> F) {
      Logger.trace("Make pivot: pivot=" + pivot + ", base_var=" + variable);
      Logger.trace("\n" + m);
      if (m.nbTrues(pivot) == 0 && m.nbUnknowns(pivot) == 1) {
         assert m.firstUndefined(pivot) != -1;
         F.add(new Affectation(m.firstUndefined(pivot), false));
      }
      if (m.nbTrues(pivot) == 1 && m.nbUnknowns(pivot) == 1) {
         assert m.firstUndefined(pivot) != -1;
         F.add(new Affectation(m.firstUndefined(pivot), true));
      }
      for (int k : m.rows()) {
         if (k != pivot && (m.isUndefined(k, variable) || m.isTrue(k, variable))) {
            boolean isValid = m.xor(k, pivot);
            if (!isValid) return false;
            if (!m.stableState()) {
               System.err.println(m);
            }
            /*
            if (m.nbTrues(k) + m.nbUnknowns(k) < 1) {
               Logger.warn("After xor line: m.nbTrues(k) + m.nbUnknowns(k) = " + (m.nbTrues(k) + m.nbUnknowns(k)));
            }
            */
            if (m.nbTrues(k) == 1 && m.nbUnknowns(k) == 0) {
               return false;
            }
            if (m.nbTrues(k) == 0 && m.nbUnknowns(k) == 1) {
               assert m.firstUndefined(k) != -1;
               F.add(new Affectation(m.firstUndefined(k), false));
            }
            if (m.nbTrues(k) == 1 && m.nbUnknowns(k) == 1) {
               assert m.firstUndefined(k) != -1;
               F.add(new Affectation(m.firstUndefined(k), true));
            }
         }
      }
      m.setBase(pivot, variable);
      assert m.stableState();
      return true;
   }

   public static boolean normalize(XORMatrix m, List<Affectation> F) {
      if (!m.stableState()) return false;
      Logger.debug("Call normalize with: \n" + m);
      removeEmptyColumns(m);
      removeEmptyLines(m);
      if (m.nbColumns() == 0) return true;

      IntIterator validRows = m.rows().iterator();
      int base = validRows.nextInt();
      int nbBases = 0;
      Logger.debug("After removing empty values: \n" + m);
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
      Logger.debug("After normalization: \n" + m);
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

   public static boolean propagateVarAssignedToFalse(XORMatrix m, int variable, List<Affectation> F) {
      assert m.stableState();
      assert !m.isFixed(variable);
      m.fix(variable, false);
      if (m.isBase(variable)) {
         int ivar = m.pivotOf(variable);
         if (m.nbTrues(ivar) == 0 && m.nbUnknowns(ivar) == 0) {
            m.removeRow(ivar);
            assert m.stableState();
         } else if (m.nbTrues(ivar) == 0 && m.nbUnknowns(ivar) == 1) {
            F.add(new Affectation(m.firstUndefined(ivar), false));
         } else {
            m.removeFromBase(variable);
            m.removeVar(variable);
            if (!makePivot(m, ivar, m.firstUndefined(ivar), F)) return false;
         }
      } else {
         for (int i : m.rows()) {
            if (m.isFalse(i, variable)) {
               if (m.nbTrues(i) != 1 || m.nbUnknowns(i) != 0) return false;
               if (m.nbTrues(i) == 0 && m.nbUnknowns(i) == 1) {
                  F.add(new Affectation(m.firstUndefined(i, variable), false));
               }
               if (m.nbTrues(i) == 1 && m.nbUnknowns(i) == 1) {
                  F.add(new Affectation(m.firstUndefined(i, variable), true));
               }
            }
         }
         m.removeVar(variable);
      }
      assert m.stableState();
      return true;
   }

   public static boolean propagateVarAssignedToTrue(XORMatrix m, int variable, List<Affectation> F) {
      assert m.stableState();
      assert !m.isFixed(variable);
      m.fix(variable, true);
      for(int i : m.rows()) {
         assert (m.nbUnknowns(i) != 0 || m.nbTrues(i) != 1);
         if(m.nbUnknowns(i) == 1 && m.nbTrues(i) == 1) {
            F.add(new Affectation(m.firstUndefined(i), true));
         }
      }
      assert m.stableState();
      return true;
   }

}

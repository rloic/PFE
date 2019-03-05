package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.util.Logger;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class Algorithms {

   private static boolean makePivot(XORMatrix m, int pivot, int variable, List<Affectation> queue) {
      assert !m.isInvalid(pivot);
      if (m.nbUnknowns(pivot) == 1) {
         assert m.firstUnknown(pivot) != -1;
         if (m.nbTrues(pivot) == 0) {
            queue.add(new Affectation(m.firstUnknown(pivot), false));
         } else if (m.nbTrues(pivot) == 1) {
            queue.add(new Affectation(m.firstUnknown(pivot), true));
         }
      }

      IntList equationsOfVariable = new IntArrayList(m.equationsOf(variable));
      for (int k : equationsOfVariable) {
         if (k != pivot) {
            boolean isValid = m.xor(k, pivot);
            if (!isValid) return false;
            assert !m.isInvalid(k);
            if (m.nbUnknowns(k) == 1) {
               assert m.firstUnknown(k) != -1;
               if (m.nbTrues(k) == 1) {
                  queue.add(new Affectation(m.firstUnknown(k), true));
               } else if (m.nbTrues(k) == 0) {
                  queue.add(new Affectation(m.firstUnknown(k), false));
               }
            }
         }
      }
      m.setBase(pivot, variable);
      assert m.stableState();
      return true;
   }

   public static boolean normalize(XORMatrix m, List<Affectation> queue) {
      if (!m.stableState()) return false;
      m.removeUnusedVariables();
      m.removeEmptyEquations();
      if (m.nbVariables() == 0) return true;

      IntIterator validRows = m.equations().iterator();
      int base = validRows.nextInt();
      int nbBases = 0;
      for (int col : m.variables()) {
         if (nbBases == m.nbEquations()) break;
         for (int row : m.equations()) {
            if (row >= base && (m.isUndefined(row, col) || m.isTrue(row, col))) {
               if (row != base) m.swap(row, base);
               if (!makePivot(m, base, col, queue)) return false;
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

   public static boolean assignToTrue(XORMatrix m, int variable, List<Affectation> queue) {
      if (!m.stableState()) {
         Logger.err("Unstable state\n" + m);
         assert false;
      }
      if (m.isFixed(variable)) {
         return m.isTrue(variable);
      }
      m.fix(variable, true);
      IntList equations;
      if (m.isBase(variable)) {
         equations = new IntArrayList(new int[]{m.pivotOf(variable)});
      } else {
         equations = m.equationsOf(variable);
      }
      for (int equation : equations) {
         if (m.isInvalid(equation)) {
            return false;
         }
         if (m.nbUnknowns(equation) == 1 && m.nbTrues(equation) == 1) {
            queue.add(new Affectation(m.firstUnknown(equation), true));
         }
      }
      assert m.stableState();
      return true;
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
         if (m.isEmptyEquation(ivar)) {
            m.removeRow(ivar);
         } else {
            if (!makePivot(m, ivar, m.firstEligiblePivot(ivar), queue)) {
               return false;
            }
         }
      } else {
         for(int equation : m.equationsOf(variable)) {
            if(m.isInvalid(equation)) {
               return false;
            }
            if (m.nbUnknowns(equation) == 1) {
               if (m.nbTrues(equation) == 1) {
                  queue.add(new Affectation(m.firstUnknown(equation), true));
               } else if (m.nbTrues(equation) == 0) {
                  queue.add(new Affectation(m.firstUnknown(equation), false));
               }
            }
         }
      }
      m.removeVar(variable);
      assert m.stableState();
      return true;
   }

}

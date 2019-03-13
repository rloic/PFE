package com.github.rloic.paper;

import com.github.rloic.inference.IAffectation;
import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.inference.impl.FalseAffectation;
import com.github.rloic.inference.impl.TrueAffectation;
import com.github.rloic.util.Logger;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.List;

public class Algorithms {

   public static boolean makePivot(
         XORMatrix m,
         int[][] reify,
         int pivot,
         int variable,
         List<IAffectation> queue
   ) {
      assert !m.isInvalid(pivot);
      if (!infer(m, reify, pivot, queue)) return false;

      IntList equationsOfVariable = new IntArrayList(m.equationsOf(variable));
      for (int equation : equationsOfVariable) {
         if (equation != pivot) {
            boolean isValid = m.xor(equation, pivot);
            if (!isValid) return false;
            if(!infer(m, reify, equation, queue)) return false;
         }
      }
      m.setBase(pivot, variable);
      assert m.stableState();
      return true;
   }

   public static boolean normalize(
         XORMatrix m,
         int[][] reify,
         List<IAffectation> queue
   ) {
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
            if (row >= base && (m.isUnknown(row, col) || m.isTrue(row, col))) {
               if (row != base) m.swap(row, base);
               if (!makePivot(m, reify, base, col, queue)) return false;
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

   public static boolean assignToTrue(
         XORMatrix m,
         int[][] reify,
         int variable,
         List<IAffectation> queue
   ) {
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
         if (!infer(m, reify, equation, queue)) return false;
      }
      assert m.stableState();
      return true;
   }

   public static boolean assignToFalse(
         XORMatrix m,
         int[][] reify,
         int variable,
         List<IAffectation> queue
   ) {
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
            if (!makePivot(m, reify, ivar, m.firstEligibleBase(ivar), queue)) {
               return false;
            }
         }
      } else {
         for(int equation : m.equationsOf(variable)) {
            if(m.isInvalid(equation)) {
               return false;
            }
            if(!infer(m, reify, equation, queue)) return false;
         }
      }
      m.removeVar(variable);
      assert m.stableState();
      return true;
   }

   private static boolean infer(
         XORMatrix m,
         int[][] reify,
         int equation,
         List<IAffectation> queue
   ) {

      if (m.nbUnknowns(equation) == 1) {
         if (m.nbTrues(equation) == 0) {
            queue.add(new FalseAffectation(m.firstUnknown(equation), reify));
         } else if (m.nbTrues(equation) == 1) {
            queue.add(new TrueAffectation(m.firstUnknown(equation)));
         }
      } else if (m.nbUnknowns(equation) == 2) {
         IntList unknowns = m.unknownsOf(equation);
         assert unknowns.size() == 2;

         int a = unknowns.getInt(0);
         int b = unknowns.getInt(1);
         int reifier = reify[a][b];

         if (reifier != -1) {
            if (m.nbTrues(equation) == 0) {
               if (m.isFixed(reifier)) {
                  return m.isFalse(reifier);
               }
               queue.add(new FalseAffectation(reifier, reify));
            } else if (m.nbTrues(equation) == 1) {
               if (m.isFixed(reifier)) {
                  return m.isTrue(reifier);
               }
               queue.add(new TrueAffectation(reifier));
            }
         }
      }

      return true;

   }

}

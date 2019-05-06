package com.github.rloic.paper.dancinglinks.rulesapplier;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.cell.Data;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public interface RulesApplier {

   IUpdater buildTrueAssignation(int variable);

   IUpdater buildFalseAssignation(int variable);

   static void gauss(IDancingLinksMatrix m) {
      boolean[] isPivot = new boolean[m.nbEquations()];
      boolean[] hadAOne = new boolean[m.nbEquations()];
      IntList conflicts = new IntArrayList();

      for (int variable = 0; variable < m.nbVariables(); variable++) {
         conflicts.clear();
         for (Data it : m.equationsOf(variable)) {
            if (!isPivot[it.equation] && !hadAOne[it.equation] && !m.isBase(variable)) {
               m.setBase(it.equation, variable);
               isPivot[it.equation] = true;
            } else {
               conflicts.add(it.equation);
            }
         }

         if (m.isBase(variable)) {
            int pivot = m.pivotOf(variable);
            for (int target : conflicts) {
               m.xor(target, pivot);
               if (!isPivot[target]) {
                  hadAOne[target] = hasAOne(m, target, variable);
               }
            }
         }
      }

      for (int equation = 0; equation < m.nbEquations(); equation++) {
         if (m.nbUnknowns(equation) == 0) {
            m.removeEquation(equation);
         }
      }

   }

   static boolean hasAOne(IDancingLinksMatrix m, int equation, int column) {
      for (Data cell : m.variablesOf(equation)) {
         if ((m.isTrue(cell.equation, cell.variable) || m.isUnknown(cell.equation, cell.variable)) && cell.variable < column) {
            return true;
         }
      }
      return false;
   }

}

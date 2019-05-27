package com.github.rloic.paper.dancinglinks.rulesapplier;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;

import java.util.function.IntConsumer;

public interface RulesApplier {

   IUpdater buildTrueAssignation(int variable);

   IUpdater buildFalseAssignation(int variable);

   static void gauss(IDancingLinksMatrix m) {
      boolean[] isPivot = new boolean[m.nbEquations()];
      boolean[] hadAOne = new boolean[m.nbEquations()];
      IntSet conflicts = new IntArraySet(m.nbEquations());

      for (int variable = 0; variable < m.nbVariables(); variable++) {
         conflicts.clear();
         for (int equation : m.equationsOf(variable)) {
            if (!isPivot[equation] && !hadAOne[equation] && !m.isBase(variable)) {
               m.setBase(equation, variable);
               isPivot[equation] = true;
            } else {
               conflicts.add(equation);
            }
         }

         if (m.isBase(variable)) {
            int pivot = m.pivotOf(variable);
            final int _variable = variable;
            conflicts.forEach((IntConsumer) target -> {
               m.xor(target, pivot);
               if (!isPivot[target]) {
                  hadAOne[target] = hasAOne(m, target, _variable);
               }
            });
         }
      }

      for (int equation = 0; equation < m.nbEquations(); equation++) {
         if (m.nbUnknowns(equation) == 0) {
            m.removeEquation(equation);
         }
      }

   }

   static boolean hasAOne(IDancingLinksMatrix m, int equation, int column) {
      for (int variable : m.variablesOf(equation)) {
         assert m.isTrue(equation, variable) == m.isTrue(variable);
         assert m.isUnknown(equation, variable) == m.isUndefined(variable);
         if ((m.isTrue(equation, variable) || m.isUnknown(equation, variable)) && variable < column) {
            return true;
         }
      }
      return false;
   }

}

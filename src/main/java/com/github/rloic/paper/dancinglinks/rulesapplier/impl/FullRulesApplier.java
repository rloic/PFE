package com.github.rloic.paper.dancinglinks.rulesapplier.impl;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Nothing;
import com.github.rloic.paper.dancinglinks.actions.UpdaterList;
import com.github.rloic.paper.dancinglinks.actions.impl.*;
import com.github.rloic.paper.dancinglinks.cell.Data;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class FullRulesApplier implements RulesApplier {

   private final InferenceEngine engine;

   public FullRulesApplier() {
      this.engine = new FullInferenceEngine();
   }

   FullRulesApplier(InferenceEngine engine) {
      this.engine = engine;
   }

   public IUpdater buildTrueAssignation(int variable) {
      return new Fix(variable, true)
            .then(matrix -> {
               if (matrix.isBase(variable)) {
                  int pivot = matrix.pivotOf(variable);
                  return inferOnlyForEquation(pivot)
                        .then(inferBasesEqualities(pivot));
               } else {
                  return inferForAllEquationsOf(matrix, variable);
               }
            });
   }

   public IUpdater buildFalseAssignation(int variable) {
      return fix(variable, false)
            .then(matrix -> {
               if (matrix.isBase(variable)) {
                  int pivot = matrix.pivotOf(variable);
                  return inferOnlyForEquation(pivot)
                        .then(
                              matrix.isEmpty(pivot) ?
                                    removeEquation(pivot) :
                                    makePivot(matrix, pivot, variable)
                        );
               } else {
                  return inferForAllEquationsOf(matrix, variable);
               }
            }).then(removeVariable(variable))
            .then(inferAllBaseEqualities(variable));
   }

   private Function<IDancingLinksMatrix, IUpdater> inferBasesEqualities(int pivot) {
      return matrix -> {
         IDancingLinksMatrix m = (IDancingLinksMatrix) matrix;
         int baseVar = m.baseVariableOf(pivot);
         if (baseVar != -1 && m.isTrue(baseVar)) {
            UpdaterList sameVar = new UpdaterList("InferBasesEqualities");
            int firstOffBase = m.minEligibleBase(pivot);
            for (Data eqCell : m.equationsOf(firstOffBase)) {
               int baseVariableOfEquation = m.baseVariableOf(eqCell.equation);
               if (
                     baseVariableOfEquation != -1
                           && !m.isTrue(baseVariableOfEquation)
                           && m.nbUnknowns(eqCell.equation) == m.nbUnknowns(pivot) + 1
                           && m.nbTrues(eqCell.equation) == m.nbTrues(pivot) - 1
                           && m.sameOffBaseVariables(eqCell.equation, pivot)
               ) {
                  sameVar.add(new InferAffectation(baseVariableOfEquation, true));
               }
            }
            return sameVar.isNotEmpty() ? sameVar : Nothing.INSTANCE;
         } else {
            return Nothing.INSTANCE;
         }
      };
   }

   final IUpdater inferOnlyForEquation(int equation) {
      return infer(equation);
   }

   final IUpdater inferForAllEquationsOf(IDancingLinksMatrix matrix, int variable) {
      UpdaterList updaterList = new UpdaterList("inferForAllEquations");
      for (Data it : matrix.equationsOf(variable)) {
         updaterList.add(infer(it.equation));
      }
      return updaterList.isNotEmpty() ? updaterList : Nothing.INSTANCE;
   }

   final IUpdater xorAndInferAllEquationsOf(IDancingLinksMatrix matrix, int pivot, int variable) {
      UpdaterList updaterList = new UpdaterList("xorAndInferForAllEquations");
      for (Data it : matrix.equationsOf(variable)) {
         if (it.equation != pivot) {
            updaterList.add(xor(it.equation, pivot));
            updaterList.add(infer(it.equation));
         }
      }
      return updaterList.isNotEmpty() ? updaterList : Nothing.INSTANCE;
   }

   final IUpdater makePivot(IDancingLinksMatrix matrix, int pivot, int oldBaseVar) {
      int newBaseVar = matrix.eligibleBase(pivot);
      return new SwapBase(oldBaseVar, newBaseVar)
            .then(it -> xorAndInferAllEquationsOf(it, pivot, newBaseVar));
   }

   final Function<IDancingLinksMatrix, IUpdater> inferAllBaseEqualities(int variable) {
      return matrix -> {
         IDancingLinksMatrix m = (IDancingLinksMatrix) matrix;
         UpdaterList updaters = new UpdaterList();

         for (Data row : m.equationsOf(variable)) {
            int pivot = row.equation;
            int base = m.baseVariableOf(pivot);
            if (base != -1) {
               int firstOffBase = m.minEligibleBase(pivot);
               if(firstOffBase != -1) {
                  if (m.isTrue(base)) {
                     for (Data targetO : m.equationsOf(firstOffBase)) {
                        int target = targetO.equation;
                        int targetBaseVar = m.baseVariableOf(target);
                        if (
                              targetBaseVar != -1
                                    && !m.isTrue(targetBaseVar)
                                    && m.nbUnknowns(target) == m.nbUnknowns(pivot) + 1
                                    && m.nbTrues(target) == m.nbTrues(pivot) - 1
                                    && m.sameOffBaseVariables(target, pivot)
                        ) {
                           updaters.add(assignation(targetBaseVar, true));
                        }
                     }
                  } else {
                     for (Data targetO : m.equationsOf(firstOffBase)) {
                        int target = targetO.equation;
                        int targetBaseVar = m.baseVariableOf(target);
                        if (
                              targetBaseVar != -1
                                    && m.isTrue(targetBaseVar)
                                    && m.nbUnknowns(target) == m.nbUnknowns(pivot) - 1
                                    && m.nbTrues(target) == m.nbTrues(pivot) + 1
                                    && m.sameOffBaseVariables(target, pivot)
                        ) {
                           updaters.add(assignation(base, true));
                           break;
                        }
                     }
                  }
               }
            }
         }
         return updaters;
      };
   }

   final IUpdater removeEquation(int equation) {
      return new RemoveEquation(equation);
   }

   final IUpdater fix(int variable, boolean value) {
      return new Fix(variable, value);
   }

   final IUpdater removeVariable(int variable) {
      return new RemoveVariable(variable);
   }

   final IUpdater infer(int equation) {
      return new InferFromEquation(engine, equation);
   }

   final IUpdater assignation(int variable, boolean value) {
      return new InferAffectation(variable, value);
   }

   final IUpdater xor(int target, int pivot) {
      return new XOR(target, pivot);
   }

   public static void gauss(IDancingLinksMatrix m) {
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

   private static boolean hasAOne(IDancingLinksMatrix m, int equation, int column) {
      for (Data cell : m.variablesOf(equation)) {
         if ((m.isTrue(cell.equation, cell.variable) || m.isUnknown(cell.equation, cell.variable)) && cell.variable < column) {
            return true;
         }
      }
      return false;
   }

}
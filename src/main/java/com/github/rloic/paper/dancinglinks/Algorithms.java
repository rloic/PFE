package com.github.rloic.paper.dancinglinks;

import com.github.rloic.paper.dancinglinks.actions.IUpdater;
import com.github.rloic.paper.dancinglinks.actions.Nothing;
import com.github.rloic.paper.dancinglinks.actions.UpdaterList;
import com.github.rloic.paper.dancinglinks.actions.impl.*;
import com.github.rloic.paper.dancinglinks.cell.Data;
import com.github.rloic.paper.dancinglinks.cell.Row;
import it.unimi.dsi.fastutil.Function;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

public class Algorithms {

   public static IUpdater buildTrueAssignation(int variable) {
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

   public static IUpdater buildFalseAssignation(int variable) {
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

   private static IUpdater replaceBase(IDancingLinksMatrix m, int pivot) {
      UpdaterList updates = new UpdaterList("replaceBase");
      int base = m.baseVariableOf(pivot);
      int firstOffBaseVar = m.firstOffBase(pivot);
      for (Data it : m.equationsOf(firstOffBaseVar)) {
         if (
               it.equation != pivot
                     && m.nbUnknowns(pivot) <= m.nbUnknowns(it.equation)
                     && m.subsetOf(pivot, it.equation)
         ) {
            updates.add(xor(it.equation, pivot));
            updates.add(infer(it.equation));
         }
      }
      updates.add(removeFromBase(pivot, base));
      updates.add(removeEquation(pivot));
      return updates;
   }

   private static Function<IDancingLinksMatrix, IUpdater> inferBasesEqualities(int pivot) {
      return matrix -> {
         IDancingLinksMatrix m = (IDancingLinksMatrix) matrix;
         int baseVar = m.baseVariableOf(pivot);
         if (baseVar != -1 && m.isTrue(baseVar)) {
            UpdaterList sameVar = new UpdaterList("InferBasesEqualities");
            int firstOffBase = m.eligibleBase(pivot);
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

   private static IUpdater inferOnlyForEquation(int equation) {
      return infer(equation);
   }

   private static IUpdater inferForAllEquationsOf(IDancingLinksMatrix matrix, int variable) {
      UpdaterList updaterList = new UpdaterList("inferForAllEquations");
      for (Data it : matrix.equationsOf(variable)) {
         updaterList.add(new InferFromEquation(it.equation));
      }
      return updaterList.isNotEmpty() ? updaterList : Nothing.INSTANCE;
   }

   private static boolean isFull(IDancingLinksMatrix m, int equation) {
      return (m.nbUnknowns(equation) == 0) || m.isEmpty(equation);
   }

   private static IUpdater inferAndRemoveIfFullForAllEquationsOf(IDancingLinksMatrix matrix, int variable) {
      UpdaterList updates = new UpdaterList("inferForAllEquations");
      for (Data it : matrix.equationsOf(variable)) {
         if (isFull(matrix, it.equation)) {
            if (matrix.nbTrues(it.equation) == 2) {
               updates.add(removeEquation(it.equation));
            } else if( matrix.nbTrues(it.equation) > 2) {
               updates.add(Nothing.INSTANCE.then(m ->replaceBase(m, it.equation)));
            }
         } else {
            updates.add(new InferFromEquation(it.equation));
         }
      }
      return updates.isNotEmpty() ? updates : Nothing.INSTANCE;
   }

   private static IUpdater xorAndInferAllEquationsOf(IDancingLinksMatrix matrix, int pivot, int variable) {
      UpdaterList updaterList = new UpdaterList("xorAndInferForAllEquations");
      for (Data it : matrix.equationsOf(variable)) {
         if (it.equation != pivot) {
            updaterList.add(new XOR(it.equation, pivot));
            updaterList.add(new InferFromEquation(it.equation));
         }
      }
      return updaterList.isNotEmpty() ? updaterList : Nothing.INSTANCE;
   }

   private static IUpdater makePivot(IDancingLinksMatrix matrix, int pivot, int oldBaseVar) {
      int newBaseVar = matrix.eligibleBase(pivot);
      return new SwapBase(oldBaseVar, newBaseVar)
            .then(it -> xorAndInferAllEquationsOf(it, pivot, newBaseVar));
   }

   private static Function<IDancingLinksMatrix, IUpdater> inferAllBaseEqualities(int variable) {
      return matrix -> {
         IDancingLinksMatrix m = (IDancingLinksMatrix) matrix;
         UpdaterList updaters = new UpdaterList();

         for (Data row : m.equationsOf(variable)) {
            int pivot = row.equation;
            int base = m.baseVariableOf(pivot);
            if (base != -1) {
               int firstOffBase = m.eligibleBase(pivot);
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

   private static IUpdater removeEquation(int equation) {
      return new RemoveEquation(equation);
   }

   private static IUpdater fix(int variable, boolean value) {
      return new Fix(variable, value);
   }

   private static IUpdater removeVariable(int variable) {
      return new RemoveVariable(variable);
   }

   private static IUpdater infer(int equation) {
      return new InferFromEquation(equation);
   }

   private static IUpdater assignation(int variable, boolean value) {
      return new InferAffectation(variable, value);
   }

   private static IUpdater xor(int target, int pivot) {
      return new XOR(target, pivot);
   }

   private static IUpdater removeFromBase(int pivot, int base) {
      return new RemoveFromBase(pivot, base);
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
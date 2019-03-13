package com.github.rloic.paper.basic.impl;

import com.github.rloic.paper.basic.XORMatrix;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

public class AdjacencyMatrixImpl implements XORMatrix {

   private static final byte FALSE = -1;
   private static final byte UNDEFINED = 0;
   private static final byte TRUE = 1;

   private static final int NO_PIVOT = -1;

   private final byte[] valueOf;
   private final IntList[] equationsOf;
   private final IntList[] variablesOf;
   private final boolean[] isBase;
   private final int[] pivotOf;
   private final IntList variables;
   private final IntList equations;
   private final int[] nbUnknowns;
   private final int[] nbTrues;

   private final int _nbVariables;
   private final int[][] _equations;

   public AdjacencyMatrixImpl(int[][] equations, int nbVariables) {
      _equations = equations;
      _nbVariables = nbVariables;

      variablesOf = new IntList[equations.length];
      equationsOf = new IntList[nbVariables];
      variables = new IntArrayList(nbVariables);
      this.equations = new IntArrayList(equations.length);
      for (int variable = 0; variable < nbVariables; variable++) {
         equationsOf[variable] = new IntArrayList();
         variables.add(variable);
      }
      for (int equationId = 0; equationId < _equations.length; equationId++) {
         int[] equation = _equations[equationId];
         Arrays.sort(equation);
         variablesOf[equationId] = new IntArrayList(equation);
         this.equations.add(equationId);
         for(int variable : equation) {
            equationsOf[variable].add(equationId);
         }
      }
      valueOf = new byte[nbVariables];
      isBase = new boolean[nbVariables];
      pivotOf = new int[nbVariables];
      nbUnknowns = new int[equations.length];
      nbTrues = new int[equations.length];
      Arrays.fill(pivotOf, NO_PIVOT);
   }

   @Override
   public int nbEquations() {
      return equations.size();
   }

   @Override
   public int nbVariables() {
      return variables.size();
   }

   @Override
   public IntList equations() {
      return equations;
   }

   @Override
   public IntList variables() {
      return variables;
   }

   @Override
   public boolean isUnknown(int equation, int variable) {
      return variablesOf[equation].contains(variable);
   }

   @Override
   public boolean isFalse(int equation, int variable) {
      return variablesOf[equation].contains(variable) && valueOf[variable] == FALSE;
   }

   @Override
   public boolean isTrue(int equation, int variable) {
      return variablesOf[equation].contains(variable) && valueOf[variable] == TRUE;
   }

   @Override
   public boolean isNone(int equation, int variable) {
      return !variablesOf[equation].contains(variable);
   }

   @Override
   public boolean isFixed(int variable) {
      return valueOf[variable] != UNDEFINED;
   }

   @Override
   public boolean isBase(int variable) {
      return isBase[variable];
   }

   @Override
   public int pivotOf(int variable) {
      assert isBase[variable];
      return pivotOf[variable];
   }

   @Override
   public void removeVar(int variable) {
      variables.rem(variable);
      for (int equation : equations) {
         variablesOf[equation].rem(variable);
      }
   }

   @Override
   public void removeRow(int equation) {
      for(int variable : variables) {
         equationsOf[variable].rem(equation);
      }
      equations.rem(equation);
   }

   @Override
   public int nbUnknowns(int equation) {
      return nbUnknowns[equation];
      /*
      int count = 0;
      for (int variable : variablesOf[equation]) {
         if (valueOf[variable] == UNDEFINED) {
            count += 1;
         }
      }
      return count;*/
   }

   @Override
   public int nbTrues(int equation) {
      return nbTrues[equation];
      /*int count = 0;
      for (int variable : variablesOf[equation]) {
         if (valueOf[variable] == TRUE) {
            count += 1;
         }
      }
      return count;*/
   }

   @Override
   public boolean isTrue(int variable) {
      return valueOf[variable] == TRUE;
   }

   @Override
   public boolean isFalse(int variable) {
      return valueOf[variable] == FALSE;
   }

   @Override
   public boolean xor(int target, int pivot) {
      for(int variable : variablesOf[target]) {
         equationsOf[variable].rem(target);
      }
      IntList xorVariables = new IntArrayList();
      int ia = 0;
      int ib = 0;
      IntList targetEquation = variablesOf[target];
      IntList pivotEquation = variablesOf[pivot];
      while (ia < targetEquation.size() && ib < pivotEquation.size()) {
         if (targetEquation.getInt(ia) < pivotEquation.getInt(ib)) {
            int variable = targetEquation.getInt(ia);
            xorVariables.add(variable);
            addSorted(equationsOf[variable], target);
            ia += 1;
         } else if (targetEquation.getInt(ia) > pivotEquation.getInt(ib)) {
            int variable = pivotEquation.getInt(ib);
            xorVariables.add(variable);
            addSorted(equationsOf[variable], target);
            ib += 1;
         } else {
            ia += 1;
            ib += 1;
         }
      }

      if (ia < targetEquation.size()) {
         for(int i = ia; i < targetEquation.size(); i++) {
            int variable = targetEquation.getInt(i);
            xorVariables.add(variable);
            addSorted(equationsOf[variable], target);
         }
      } else if (ib < pivotEquation.size()) {
         for(int i = ib; i < pivotEquation.size(); i++) {
            int variable = pivotEquation.getInt(i);
            xorVariables.add(variable);
            addSorted(equationsOf[variable], target);
         }
      }

      variablesOf[target] = xorVariables;
      nbUnknowns[target] = 0;
      nbTrues[target] = 0;
      for (int variable : variablesOf[target]) {
         if(valueOf[variable] == UNDEFINED) {
            nbUnknowns[target] += 1;
         } else if (valueOf[variable] == TRUE) {
            nbTrues[target] += 1;
         }
      }
      if(isInvalid(target)) {
         xor(target, pivot);
         return false;
      }
      return true;
   }

   @Override
   public void setBase(int pivot, int variable) {
      isBase[variable] = true;
      pivotOf[variable] = pivot;
   }

   @Override
   public void removeFromBase(int variable) {
      isBase[variable] = false;
      pivotOf[variable] = NO_PIVOT;
   }

   private void addSorted(IntList list, int element) {
      int i = 0;
      while (i < list.size() && list.getInt(i) < element) {
         i++;
      }
      if (i == list.size()) {
         list.add(element);
      } else {
         list.add(i, element);
      }
   }

   @Override
   public void swap(int equationA, int equationB) {
      for(int variable : variablesOf[equationB]) {
         equationsOf[variable].rem(equationB);
         addSorted(equationsOf[variable], equationA);
      }
      for (int variable : variablesOf[equationA]) {
         equationsOf[variable].rem(equationA);
         addSorted(equationsOf[variable], equationB);
      }
      IntList variableOfA = variablesOf[equationA];
      variablesOf[equationA] = variablesOf[equationB];
      variablesOf[equationB] = variableOfA;

      int nbTruesOfA = nbTrues[equationA];
      int nbUnknownsOfA = nbUnknowns[equationA];
      nbTrues[equationA] = nbTrues[equationB];
      nbUnknowns[equationA] = nbUnknowns[equationB];
      nbTrues[equationB] = nbTruesOfA;
      nbUnknowns[equationB] = nbUnknownsOfA;
   }

   @Override
   public void fix(int variable, boolean value) {
      assert valueOf[variable] == UNDEFINED;
      for(int equation : equationsOf[variable]) {
         nbUnknowns[equation] -= 1;
         if(value) {
            nbTrues[equation] += 1;
         }
      }
      valueOf[variable] = value ? TRUE : FALSE;
   }

   @Override
   public int firstUnknown(int equation) {
      for(int variable : variablesOf[equation]) {
         if (valueOf[variable] == UNDEFINED) {
            return variable;
         }
      }
      return -1;
   }

   @Override
   public IntList equationsOf(int variable) {
      return equationsOf[variable];
   }

   @Override
   public IntList unknownsOf(int equation) {
      IntList unknowns = new IntArrayList();
      for(int variable : variablesOf[equation]) {
         if (!isFixed(variable)) {
            unknowns.add(variable);
         }
      }
      return unknowns;
   }

   @Override
   public int firstEligibleBase(int equation) {
      for(int variable : variablesOf[equation]) {
         if (valueOf[variable] == UNDEFINED || valueOf[variable] == TRUE && !isBase[variable]) {
            return variable;
         }
      }
      return -1;
   }

   @Override
   public boolean stableState() {
      for(int equation : equations) {
         if (isInvalid(equation)) {
            return false;
         }
      }
      return true;
   }

   @Override
   public void clear() {
      variables.clear();
      equations.clear();
      for (int variable = 0; variable < _nbVariables; variable++) {
         equationsOf[variable].clear();
         variables.add(variable);
      }
      Arrays.fill(nbUnknowns, 0);
      for (int equationId = 0; equationId < _equations.length; equationId++) {
         int[] equation = _equations[equationId];
         variablesOf[equationId] = new IntArrayList(equation);
         nbUnknowns[equationId] = equation.length;
         equations.add(equationId);
         for(int variable : equation) {
            equationsOf[variable].add(equationId);
         }
      }
      Arrays.fill(nbTrues, 0);
      Arrays.fill(valueOf, UNDEFINED);
      Arrays.fill(isBase, false);
      Arrays.fill(pivotOf, NO_PIVOT);
   }

   @Override
   public void removeEmptyEquations() {
      int nbEquations = equations.size();
      for(int equation = 0; equation < nbEquations; equation++) {
         boolean remove = true;
         for(int variable : variablesOf[equation]) {
            if (valueOf[variable] != FALSE) {
               remove = false;
            }
         }
         if(remove) {
            removeRow(equation);
         }
      }
   }

   @Override
   public void removeUnusedVariables() {
      int nbVariables = variables.size();
      for(int variable = 0; variable < nbVariables; variable++) {
         if (equationsOf[variable].isEmpty() || valueOf[variable] == FALSE) {
            removeVar(variable);
         }
      }
   }

   @Override
   public String toString() {
      StringBuilder str = new StringBuilder();
      str.append('\t');
      for (int col : variables) {
         if (col < 10) {
            str.append(' ');
         }
         str.append(col).append(' ');
      }
      str.append('\n');
      for (int i : equations) {
         str.append(i)
               .append('\t');
         for (int j : variables) {
            boolean isPivot = isBase[j] && pivotOf[j] == i;
            str.append(isPivot ? '(' : ' ');
            if (isNone(i, j)) {
               str.append('_');
            } else if (isTrue(i, j)) {
               str.append('1');
            } else if (isFalse(i, j)) {
               str.append('0');
            } else if (isUnknown(i, j)) {
               str.append('x');
            }
            str.append(isPivot ? ')' : ' ');
         }
         str.append(" | nbUnknowns: ")
               .append(nbUnknowns(i))
               .append(" | nbTrues: ")
               .append(nbTrues(i))
               .append(" | stable: ")
               .append(!isInvalid(i))
               .append('\n');
      }
      return str.toString();
   }

}

package com.github.rloic.paper.impl;

import com.github.rloic.paper.XORMatrix;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

public class NaiveMatrixImpl implements XORMatrix {

   private static final byte UNDEFINED = 0;
   private static final byte TRUE = 1;
   private static final byte FALSE = -1;

   private static final int NO_PIVOT = -1;

   private final int[] nbUnknowns;
   private final int[] nbTrues;
   private final boolean[][] data;
   private final byte[] valueOf;
   private final boolean[] isBase;
   private final int[] pivotOf;

   private final IntList rows;
   private final IntList columns;

   private final int[][] equations;
   private int nbVariables;

   public NaiveMatrixImpl(int[][] equations, int nbVariables) {
      this.equations = equations;
      this.nbVariables = nbVariables;
      rows = new IntArrayList(equations.length);
      for (int i = 0; i < equations.length; i++) {
         rows.add(i);
      }
      columns = new IntArrayList(nbVariables);
      for (int j = 0; j < nbVariables; j++) {
         columns.add(j);
      }
      int nbRows = equations.length;
      nbUnknowns = new int[nbRows];
      nbTrues = new int[nbRows];
      data = new boolean[nbRows][];
      for (int i = 0; i < nbRows; i++) {
         data[i] = new boolean[nbVariables];
         for (int j : equations[i]) {
            data[i][j] = true;
            nbUnknowns[i] += 1;
         }
      }
      valueOf = new byte[nbVariables];
      isBase = new boolean[nbVariables];
      pivotOf = new int[nbVariables];
      Arrays.fill(pivotOf, NO_PIVOT);
   }

   @Override
   public int nbEquations() {
      return rows.size();
   }

   @Override
   public int nbVariables() {
      return columns.size();
   }

   @Override
   public IntList equations() {
      return rows;
   }

   @Override
   public IntList variables() {
      return columns;
   }

   @Override
   public boolean isUndefined(int row, int col) {
      assert row >= 0 && row < equations.length;
      assert col >= 0 && col < nbVariables;
      return data[row][col] && valueOf[col] == UNDEFINED;
   }

   @Override
   public boolean isFalse(int row, int col) {
      return data[row][col] && valueOf[col] == FALSE;
   }

   @Override
   public boolean isTrue(int row, int col) {
      return data[row][col] && valueOf[col] == TRUE;
   }

   @Override
   public boolean isNone(int row, int col) {
      return !data[row][col];
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
      assert isBase(variable);
      return pivotOf[variable];
   }

   @Override
   public void removeVar(int col) {
      assert columns.contains(col);
      for (int row : rows) {
         assert isNone(row, col) || isFalse(row, col);
      }
      assert !isBase[col];
      assert pivotOf[col] == NO_PIVOT;
      columns.rem(col);
   }

   @Override
   public void removeRow(int row) {
      assert rows.contains(row);
      for (int col : columns) {
         assert isNone(row, col) || isFalse(row, col);
      }
      rows.rem(row);
   }

   @Override
   public int nbUnknowns(int row) {
      return nbUnknowns[row];
   }

   @Override
   public int nbTrues(int row) {
      return nbTrues[row];
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
      int nbUnknownsOfTarget = 0;
      int nbTruesOfTarget = 0;
      for (int j : columns) {
         data[target][j] = data[target][j] != data[pivot][j];
         if (isUndefined(target, j)) {
            nbUnknownsOfTarget += 1;
         } else if (isTrue(target, j)) {
            nbTruesOfTarget += 1;
         }
      }
      nbUnknowns[target] = nbUnknownsOfTarget;
      nbTrues[target] = nbTruesOfTarget;
      return nbTruesOfTarget != 1 || nbUnknownsOfTarget != 0;
   }

   @Override
   public void setBase(int pivot, int variable) {
      assert !isBase[variable];
      assert pivotOf[variable] == -1;
      pivotOf[variable] = pivot;
      isBase[variable] = true;
      for(int i: rows) {
         assert isNone(i, variable) || i == pivotOf[variable];
      }
   }

   @Override
   public void removeFromBase(int variable) {
      pivotOf[variable] = NO_PIVOT;
      isBase[variable] = false;
   }

   @Override
   public void swap(int equationA, int equationB) {
      int nbTruesOfA = nbTrues[equationA];
      int nbUnknownsOfA = nbUnknowns[equationA];
      boolean[] dataOfRowA = data[equationA];
      nbTrues[equationA] = nbTrues[equationB];
      nbUnknowns[equationA] = nbUnknowns[equationB];
      data[equationA] = data[equationB];
      nbTrues[equationB] = nbTruesOfA;
      nbUnknowns[equationB] = nbUnknownsOfA;
      data[equationB] = dataOfRowA;
   }

   @Override
   public void fix(int variable, boolean value) {
      for (int row : rows) {
         if (isUndefined(row, variable)) {
            nbUnknowns[row] -= 1;
            if (value) {
               nbTrues[row] += 1;
            }
         }
      }
      valueOf[variable] = value ? TRUE : FALSE;
   }

   @Override
   public int firstUnknown(int equation) {
      assert nbUnknowns[equation] > 0;
      for (int j : columns) {
         if (isUndefined(equation, j)) return j;
      }
      return -1;
   }

   @Override
   public int firstEligiblePivot(int equation) {
      for (int j : columns) {
         if (isUndefined(equation, j) || isTrue(equation, j) && !isBase(j)) {
            return j;
         }
      }
      assert false : "Non eligible pivot on this row";
      return -1;
   }

   @Override
   public boolean stableState() {
      for (int row : rows) {
         if (nbUnknowns(row) == 0 && nbTrues(row) == 1) return false;
      }
      return true;
   }

   @Override
   public void clear() {
      rows.clear();
      for (int i = 0; i < equations.length; i++) {
         rows.add(i);
      }
      columns.clear();
      for (int j = 0; j < nbVariables; j++) {
         columns.add(j);
      }
      int nbRows = equations.length;
      Arrays.fill(nbUnknowns, 0);
      Arrays.fill(nbTrues, 0);
      for (int i = 0; i < nbRows; i++) {
         Arrays.fill(data[i], false);
         for (int j : equations[i]) {
            data[i][j] = true;
            nbUnknowns[i] += 1;
         }
      }
      Arrays.fill(valueOf, UNDEFINED);
      Arrays.fill(isBase, false);
      Arrays.fill(pivotOf, NO_PIVOT);
   }

   @Override
   public void removeEmptyEquations() {
      int initialColumns = nbVariables();
      for (int col = 0; col < initialColumns; col++) {
         boolean removable = true;
         for (int row : rows) {
            if (isUndefined(row, col) || isTrue(row, col)) {
               removable = false;
               break;
            }
         }
         if (removable) {
            removeVar(col);
         }
      }
   }

   @Override
   public void removeUnusedVariables() {
      int initialRows = nbEquations();
      for (int row = 0; row < initialRows; row++) {
         boolean removable = true;
         for (int col : columns) {
            if (isUndefined(row, col) || isTrue(row, col)) {
               removable = false;
            }
         }
         if (removable) {
            removeRow(row);
         }
      }
   }

   @Override
   public IntList equationsOf(int variable) {
      IntList equations = new IntArrayList();
      for(int equation : rows) {
         if (data[equation][variable]) {
            equations.add(equation);
         }
      }
      return equations;
   }

   @Override
   public IntList unknownsOf(int equation) {
      IntList unknowns = new IntArrayList();
      for (int variable: columns) {
         if (data[equation][variable] && valueOf[variable] == UNDEFINED) {
            unknowns.add(variable);
         }
      }
      return unknowns;
   }

   @Override
   public String toString() {
      StringBuilder str = new StringBuilder();
      str.append('\t');
      for (int col : columns) {
         if (col < 10) {
            str.append(' ');
         }
         str.append(col).append(' ');
      }
      str.append('\n');
      for (int i : rows) {
         str.append(i)
               .append('\t');
         for (int j : columns) {
            boolean isPivot = isBase[j] && pivotOf[j] == i;
            str.append(isPivot ? '(' : ' ');
            if (isNone(i, j)) {
               str.append('_');
            } else if (isTrue(i, j)) {
               str.append('1');
            } else if (isFalse(i, j)) {
               str.append('0');
            } else if (isUndefined(i, j)) {
               str.append('x');
            }
            str.append(isPivot ? ')' : ' ');
         }
         str.append(" | nbUnknowns: ")
               .append(nbUnknowns[i])
               .append(" | nbTrues: ")
               .append(nbTrues[i])
               .append(" | stable: ")
               .append(!isInvalid(i))
               .append('\n');
      }
      return str.toString();
   }
}

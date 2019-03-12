package com.github.rloic.paper.impl.dancinglinks.dancinglinks;

import com.github.rloic.paper.impl.dancinglinks.IDancingLinksMatrix;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Predicate;

public class DancingLinksMatrix implements IDancingLinksMatrix {

   private static byte UNDEFINED = 0;
   private static byte FALSE = -1;
   private static byte TRUE = 1;

   private final Cell.Header[] equations;
   private final Cell.Header[] variables;
   private final Cell.Data[][] cells;

   private final boolean[] isBase;
   private final int[] pivotOf;
   private final int[] baseOf;
   private final byte[] valueOf;

   private final int[] nbUnknowns;
   private final int[] nbTrues;

   private final int nbEquations;
   private final int nbVariables;

   private static final int NO_PIVOT = -1;
   private static final int NO_BASE = -1;

   public DancingLinksMatrix(
         int[][] equations,
         int nbVariables
   ) {
      this.nbEquations = equations.length;
      this.nbVariables = nbVariables;
      this.valueOf = new byte[nbVariables];
      this.equations = new Cell.Header[equations.length];
      this.equations[0] = new Cell.Header("row_0");
      for (int i = 1; i < equations.length; i++) {
         this.equations[i] = new Cell.Header("row_" + i);
         this.equations[i - 1].addBottom(this.equations[i]);
      }
      variables = new Cell.Header[nbVariables];
      variables[0] = new Cell.Header("col_0");
      for (int j = 1; j < nbVariables; j++) {
         variables[j] = new Cell.Header("col_" + j);
         variables[j - 1].addRight(variables[j]);
      }

      isBase = new boolean[variables.length];
      pivotOf = new int[variables.length];
      Arrays.fill(pivotOf, NO_PIVOT);
      baseOf = new int[equations.length];
      Arrays.fill(baseOf, NO_BASE);
      nbUnknowns = new int[variables.length];
      nbTrues = new int[variables.length];

      cells = new Cell.Data[equations.length][nbVariables];
      for (int i = 0; i < equations.length; i++) {
         nbUnknowns[i] = equations[i].length;
         for (int j = 0; j < equations[i].length; j++) {
            Arrays.sort(equations[i]);
            int iVar = equations[i][j];
            Cell.Data varCell = get(i, iVar);
            Cell lastEquationOfVariable = variables[iVar].top();
            lastEquationOfVariable.addBottom(varCell);
            Cell lastVariableOfEquation = this.equations[i].left();
            lastVariableOfEquation.addRight(varCell);
         }
      }
   }

   @Override
   public boolean isUnknown(int equation, int variable) {
      return valueOf[variable] == UNDEFINED
            && get(equation, variable).isActive();
   }

   @Override
   public boolean isTrue(int equation, int variable) {
      return valueOf[variable] == TRUE
            && get(equation, variable).isActive();
   }

   @Override
   public boolean isTrue(int variable) {
      return valueOf[variable] == TRUE;
   }

   @Override
   public boolean isFalse(int equation, int variable) {
      return valueOf[variable] == FALSE
            && get(equation, variable).isActive();
   }

   @Override
   public boolean isFalse(int variable) {
      return valueOf[variable] == FALSE;
   }

   @Override
   public boolean isNone(int equation, int variable) {
      return !get(equation, variable).isActive();
   }


   @Override
   public void setBase(int pivot, int variable) {
      isBase[variable] = true;
      pivotOf[variable] = pivot;
   }

   @Override
   public void setOffBase(int variable) {
      isBase[variable] = false;
      pivotOf[variable] = NO_PIVOT;
   }

   @Override
   public int nbTrues(int equation) {
      return nbTrues[equation];
   }

   @Override
   public int nbUnknowns(int equation) {
      return nbUnknowns[equation];
   }

   @Override
   public boolean isValid(int equation) {
      return nbUnknowns[equation] != 0 || nbTrues[equation] != 1;
   }

   @Override
   public boolean isInvalid(int equation) {
      return !isValid(equation);
   }

   @Override
   public boolean isEmpty(int equation) {
      return nbTrues[equation] == 0 && nbUnknowns[equation] == 0;
   }

   @Override
   public boolean isBase(int variable) {
      return isBase[variable];
   }

   @Override
   public boolean isUnused(int variable) {
      return variables[variable].bottom() == variables[variable] || valueOf[variable] == FALSE;
   }

   @Override
   public Iterable<Integer> equationsOf(int variable) {
      return () -> new Iterator<Integer>() {
         Cell cell = variables[variable];

         @Override
         public boolean hasNext() {
            return cell.bottom() instanceof Cell.Data;
         }

         @Override
         public Integer next() {
            cell = cell.bottom();
            return ((Cell.Data) cell).equation;
         }
      };
   }

   private Cell.Data get(int equation, int variable) {
      if (cells[equation][variable] == null) {
         cells[equation][variable] = new Cell.Data(equation, variable);
      }
      return cells[equation][variable];
   }

   @Override
   public void removeEquation(int equation) {
      Cell header = equations[equation];
      Cell cell = header;
      do {
         cell.top().unlinkBottom();
         cell = cell.right();
      } while (cell != header);
   }

   @Override
   public void restoreEquation(int equation) {
      Cell header = equations[equation];
      Cell cell = header;
      do {
         cell.top().addBottom(cell);
         cell = cell.left();
      } while (cell != header);
   }

   @Override
   public void removeVariable(int variable) {
      Cell header = variables[variable];
      Cell cell = header;
      do {
         cell.left().unlinkRight();
         cell = cell.bottom();
      } while (cell != header);
   }

   @Override
   public void restoreVariable(int variable) {
      Cell header = variables[variable];
      Cell cell = header;
      do {
         cell.left().addRight(cell);
         cell = cell.top();
      } while (cell != header);
   }

   @Override
   public void xor(int target, int pivot) {
      Cell headerT = equations[target];
      Cell headerP = equations[pivot];

      Cell cellT = headerT.right();
      Cell cellP = headerP.right();

      while (cellT != headerT && cellP != headerP) {
         if (cellT.isOnTheRightOf(cellP)) {
            int variable = ((Cell.Data) cellP).variable;
            Cell.Data newUnknown = get(target, variable);
            Cell top = findLastInColumn(variable, it -> it.isAbove(newUnknown));
            Cell left = cellT.left();
            linkCell(top, left, newUnknown);

            if(isTrue(variable)) {
               nbTrues[target] += 1;
            } else if (isUndefined(variable)) {
               nbUnknowns[target] += 1;
            }

            cellP = cellP.right();
         } else if (cellP.isOnTheRightOf(cellT)) {
            cellT = cellT.right();
         } else {
            int variable = ((Cell.Data) cellT).variable;
            if (isTrue(variable)) {
               nbTrues[target] -= 1;
            } else if (isUndefined(variable)) {
               nbUnknowns[target] -= 1;
            }
            cellT.left().unlinkRight();
            cellT.top().unlinkBottom();
            cellT = cellT.right();
            cellP = cellP.right();
         }
      }

      while (cellP != headerP) {
         int variable = ((Cell.Data) cellP).variable;
         Cell.Data newUnknown = get(target, variable);
         Cell top = findLastInColumn(variable, it -> it.isAbove(newUnknown));
         Cell left = headerT.left();
         linkCell(top, left, newUnknown);

         if(isTrue(variable)) {
            nbTrues[target] += 1;
         } else if (isUndefined(variable)) {
            nbUnknowns[target] += 1;
         }

         cellP = cellP.right();
      }

   }

   private Cell findLastInColumn(int y, Predicate<Cell.Data> predicate) {
      Cell header = variables[y];
      Cell cell = header.top();
      while (cell instanceof Cell.Data && !predicate.test((Cell.Data) cell)) {
         cell = cell.top();
      }
      return cell;
   }

   private void linkCell(Cell top, Cell left, Cell unknown) {
      top.addBottom(unknown);
      left.addRight(unknown);
   }

   @Override
   public String toString() {
      StringBuilder str = new StringBuilder("    ");
      for (int j = 0; j < variables.length; j++) {
         if (variables[j].isActive()) {
            if (variables[j].isSeed()) {
               str.append(" [s] ");
            } else {
               str.append(" [*] ");
            }
         } else {
            str.append(" [ ] ");
         }
      }
      str.append('\n');
      for (int i = 0; i < equations.length; i++) {
         if (equations[i].isActive()) {
            if (equations[i].isSeed()) {
               str.append("[s] ");
            } else {
               str.append("[*] ");
            }
         } else {
            str.append("[ ] ");
         }
         for (int j = 0; j < variables.length; j++) {
            if (cells[i][j] != null && cells[i][j].isActive()) {
               byte value = valueOf[cells[i][j].variable];
               str.append(debugAndTrim(value, isBase[j]));
            } else {
               str.append("  _  ");
            }
         }
         str
               .append(" | nbUnknowns: ")
               .append(nbUnknowns(i))
               .append(" | nbTrues: ")
               .append(nbTrues(i))
               .append('\n');
      }
      return str.toString();
   }

   private String debugAndTrim(byte value, boolean base) {
      if (base) {
         if (value != UNDEFINED) {
            if (value == TRUE) {
               return " (1) ";
            } else {
               return " (0) ";
            }
         } else {
            return " (x) ";
         }
      } else {
         if (value != UNDEFINED) {
            if (value == TRUE) {
               return "  1  ";
            } else {
               return "  0  ";
            }
         } else {
            return "  x  ";
         }
      }
   }

   @Override
   public int pivotOf(int variable) {
      return pivotOf[variable];
   }

   @Override
   public void set(int variable, boolean value) {
      valueOf[variable] = value ? TRUE : FALSE;
      for(int equation : equationsOf(variable)) {
         nbUnknowns[equation] -= 1;
         if(value) {
            nbTrues[equation] += 1;
         }
      }
   }

   @Override
   public void unSet(int variable) {
      for(int equation : equationsOf(variable)) {
         nbUnknowns[equation] += 1;
         if(valueOf[variable] == TRUE) {
            nbTrues[equation] -= 1;
         }
      }
      valueOf[variable] = UNDEFINED;
   }

   @Override
   public boolean isUndefined(int variable) {
      return valueOf[variable] == UNDEFINED;
   }

   @Override
   public int eligibleBase(int pivot) {
      Cell cell = equations[pivot].right();
      while (cell instanceof Cell.Data) {
         Cell.Data data = (Cell.Data) cell;
         byte cellValue = valueOf[data.variable];
         if((cellValue == TRUE || cellValue == UNDEFINED) && !isBase[data.variable]) {
            return data.variable;
         }
         cell = cell.right();
      }
      return -1;
   }

   @Override
   public Iterable<Integer> equations() {
      return null;
   }

   @Override
   public int firstUnknown(int equation) {
      Cell cell = equations[equation].right();
      while (cell instanceof Cell.Data && valueOf[((Cell.Data) cell).variable] != UNDEFINED) {
         cell = cell.right();
      }
      if(cell instanceof Cell.Data) {
         return ((Cell.Data) cell).variable;
      }
      return -1;
   }

   @Override
   public int nbEquations() {
      return nbEquations;
   }

   @Override
   public int nbVariables() {
      return nbVariables;
   }

   @Override
   public Iterable<Cell.Data> variablesOf(int target) {
      return () -> new Iterator<Cell.Data>() {
         Cell cell = equations[target];
         @Override
         public boolean hasNext() {
            return cell.right() instanceof Cell.Data;
         }

         @Override
         public Cell.Data next() {
            cell = cell.right();
            return (Cell.Data) cell;
         }
      };
   }
}

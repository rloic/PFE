package com.github.rloic.paper.dancinglinks.impl;

import com.github.rloic.paper.dancinglinks.IDancingLinksMatrix;
import com.github.rloic.paper.dancinglinks.cell.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;
import java.util.function.Predicate;

public class DancingLinksMatrix implements IDancingLinksMatrix {

   private static byte UNDEFINED = 0;
   private static byte FALSE = -1;
   private static byte TRUE = 1;

   private final Root root;
   private final Row[] variablesOf;
   private final Column[] equationsOf;
   private final Data[][] cells;

   private final boolean[] isBase;
   private final int[] pivotOf;
   private final int[] baseOf;
   private final byte[] valueOf;

   private final int[] nbUnknowns;
   private final int[] nbTrues;

   private final int nbEquations;
   private final int nbVariables;

   private int numberOfUndefinedVariables;

   private static final int NO_PIVOT = -1;
   private static final int NO_BASE = -1;

   public DancingLinksMatrix(
         int[][] equations,
         int nbVariables
   ) {
      this.nbEquations = equations.length;
      this.nbVariables = nbVariables;
      this.numberOfUndefinedVariables = nbVariables;
      valueOf = new byte[nbVariables];

      root = new Root();
      variablesOf = new Row[nbEquations];
      if (equations.length > 0) {
         variablesOf[0] = new Row(root);
         for (int equation = 1; equation < nbEquations; equation++) {
            variablesOf[equation] = new Row(variablesOf[equation - 1]);
         }
      }
      assert variablesOf[nbEquations - 1].bottom() == root;

      equationsOf = new Column[nbVariables];
      if (nbVariables > 0) {
         equationsOf[0] = new Column(root);
         for (int variable = 1; variable < nbVariables; variable++) {
            equationsOf[variable] = new Column(equationsOf[variable - 1]);
         }
      }
      assert equationsOf[nbVariables - 1].right() == root;

      isBase = new boolean[nbVariables];
      pivotOf = new int[nbVariables];
      Arrays.fill(pivotOf, NO_PIVOT);
      baseOf = new int[equations.length];
      Arrays.fill(baseOf, NO_BASE);
      nbUnknowns = new int[nbEquations];
      nbTrues = new int[nbEquations];

      cells = new Data[nbEquations][nbVariables];
      for (int i = 0; i < equations.length; i++) {
         nbUnknowns[i] = equations[i].length;
         Arrays.sort(equations[i]);
         for (int variable : equations[i]) {
            Cell lastEquationOfVariable = equationsOf[variable].top();
            Cell lastVariableOfEquation = variablesOf[i].left();

            if (
                  lastVariableOfEquation instanceof Row
                        && lastEquationOfVariable instanceof Column
            ) {
               cells[i][variable] = new Data(i, variable, (Row) lastVariableOfEquation, (Column) lastEquationOfVariable);
            } else if (
                  lastVariableOfEquation instanceof Data
                        && lastEquationOfVariable instanceof Column
            ) {
               cells[i][variable] = new Data(i, variable, (Data) lastVariableOfEquation, (Column) lastEquationOfVariable);
            } else if (
                  lastVariableOfEquation instanceof Row
                        && lastEquationOfVariable instanceof Data
            ) {
               cells[i][variable] = new Data(i, variable, (Row) lastVariableOfEquation, (Data) lastEquationOfVariable);
            } else if (
                  lastVariableOfEquation instanceof Data
                        && lastEquationOfVariable instanceof Data
            ) {
               cells[i][variable] = new Data(i, variable, (Data) lastVariableOfEquation, (Data) lastEquationOfVariable);
            } else {
               throw new RuntimeException();
            }
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
      baseOf[pivot] = variable;
   }

   @Override
   public void setOffBase(int variable) {
      isBase[variable] = false;
      baseOf[pivotOf[variable]] = NO_BASE;
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
      return equationsOf[variable].bottom() == equationsOf[variable] || valueOf[variable] == FALSE;
   }

   @Override
   public Iterable<Data> equationsOf(int variable) {
      return equationsOf[variable];
   }

   private Data get(int equation, int variable) {
      if (cells[equation][variable] == null) {
         cells[equation][variable] = new Data(equation, variable);
      }
      return cells[equation][variable];
   }

   @Override
   public void removeEquation(int equation) {
      variablesOf[equation].remove();
   }

   @Override
   public void restoreEquation(int equation) {
      variablesOf[equation].restore();
   }

   @Override
   public void removeVariable(int variable) {
      equationsOf[variable].remove();
   }

   @Override
   public void restoreVariable(int variable) {
      equationsOf[variable].restore();
   }

   @Override
   public void xor(int target, int pivot) {
      Cell cellT = variablesOf[target].right();
      Cell cellP = variablesOf[pivot].right();

      while (cellT instanceof Data && cellP instanceof Data) {
         Data dataT = (Data) cellT;
         Data dataP = (Data) cellP;

         if (dataT.isOnTheRightOf(dataP)) {
            int variable = dataP.variable;
            Data newUnknown = get(target, variable);
            Cell top = findLastInColumn(variable, it -> it.isAboveOf(newUnknown));
            Cell left = cellT.left();
            newUnknown.relink(left, top);

            if (isTrue(variable)) {
               nbTrues[target] += 1;
            } else if (isUndefined(variable)) {
               nbUnknowns[target] += 1;
            }

            cellP = cellP.right();
         } else if (dataP.isOnTheRightOf(dataT)) {
            cellT = cellT.right();
         } else {
            int variable = dataT.variable;
            if (isTrue(variable)) {
               nbTrues[target] -= 1;
            } else if (isUndefined(variable)) {
               nbUnknowns[target] -= 1;
            }
            dataT.remove();
            cellT = cellT.right();
            cellP = cellP.right();
         }
      }

      Row headerP = variablesOf[pivot];
      Row headerT = variablesOf[target];
      while (cellP != headerP) {
         int variable = ((Data) cellP).variable;
         Data newUnknown = get(target, variable);
         Cell top = findLastInColumn(variable, it -> it.isAboveOf(newUnknown));
         Cell left = headerT.left();
         newUnknown.relink(left, top);

         if (isTrue(variable)) {
            nbTrues[target] += 1;
         } else if (isUndefined(variable)) {
            nbUnknowns[target] += 1;
         }

         cellP = cellP.right();
      }

   }

   private Cell findLastInColumn(int y, Predicate<Data> predicate) {
      Cell header = equationsOf[y];
      Cell cell = header.top();
      while (cell instanceof Data && !predicate.test((Data) cell)) {
         cell = cell.top();
      }
      return cell;
   }

   @Override
   public String toString() {
      StringBuilder str = new StringBuilder("    ");
      for (int j = 0; j < equationsOf.length; j++) {
         if (equationsOf[j].isActive()) {
            if (equationsOf[j].isSeed()) {
               str.append(" [s] ");
            } else {
               str.append(" [*] ");
            }
         } else {
            str.append(" [ ] ");
         }
      }
      str.append('\n');
      for (int i = 0; i < variablesOf.length; i++) {
         if (variablesOf[i].isActive()) {
            if (variablesOf[i].isSeed()) {
               str.append("[s] ");
            } else {
               str.append("[*] ");
            }
         } else {
            str.append("[ ] ");
         }
         for (int j = 0; j < equationsOf.length; j++) {
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
      numberOfUndefinedVariables -= 1;
      valueOf[variable] = value ? TRUE : FALSE;
      int incNbTrue = value ? 1 : 0;
      for (Data it : equationsOf(variable)) {
         nbUnknowns[it.equation] -= 1;
         nbTrues[it.equation] += incNbTrue;
      }
   }

   @Override
   public void unSet(int variable) {
      numberOfUndefinedVariables += 1;
      int decNbTrue = valueOf[variable] == TRUE ? 1 : 0;
      for (Data it : equationsOf(variable)) {
         nbUnknowns[it.equation] += 1;
         nbTrues[it.equation] -= decNbTrue;
      }

      valueOf[variable] = UNDEFINED;
   }

   @Override
   public boolean isUndefined(int variable) {
      return valueOf[variable] == UNDEFINED;
   }

   @Override
   public int eligibleBase(int pivot) {
      for (Data it : variablesOf[pivot]) {
         byte value = valueOf[it.variable];
         if ((value == TRUE || value == UNDEFINED) && !isBase[it.variable]) {
            return it.variable;
         }
      }
      return -1;
   }

   @Override
   public int firstUnknown(int equation) {
      for (Data it : variablesOf[equation]) {
         byte value = valueOf[it.variable];
         if (value == UNDEFINED) {
            return it.variable;
         }
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
   public Iterable<Data> variablesOf(int equation) {
      return variablesOf[equation];
   }

   @Override
   public IntList offBase() {
      IntList offBase = new IntArrayList();
      for (int variable = 0; variable < nbVariables; variable++) {
         if (!isBase[variable]) {
            offBase.add(variable);
         }
      }
      return offBase;
   }

   @Override
   public boolean sameOffBaseVariables(int eq1, int eq2) {
      return sameOffBaseVariables(variablesOf[eq1], variablesOf[eq2]);
   }

   @Override
   public boolean sameOffBaseVariables(Row eq1, Row eq2) {
      Cell cVarEq1 = eq1.right();
      Cell cVarEq2 = eq2.right();

      while (cVarEq1 instanceof Data && cVarEq2 instanceof Data) {
         Data varEq1 = (Data) cVarEq1;
         Data varEq2 = (Data) cVarEq2;

         if (isBase[varEq1.variable] || isBase[varEq2.variable]) {
            if (isBase[varEq1.variable]) {
               cVarEq1 = cVarEq1.right();
            }
            if (isBase[varEq2.variable]) {
               cVarEq2 = cVarEq2.right();
            }
         } else {
            if (varEq1.variable != varEq2.variable) {
               break;
            }

            cVarEq1 = cVarEq1.right();
            cVarEq2 = cVarEq2.right();
         }
      }

      if (cVarEq1 instanceof Data && isBase[((Data) cVarEq1).variable]) {
         cVarEq1 = cVarEq1.right();
      }

      if (cVarEq2 instanceof Data && isBase[((Data) cVarEq2).variable]) {
         cVarEq2 = cVarEq2.right();
      }

      return (cVarEq1 instanceof Row && cVarEq2 instanceof Row);
   }

   @Override
   public boolean sameOffBaseVariables(int eq1, int eq2, int ignoreVar) {
      Cell cVarEq1 = variablesOf[eq1].right();
      Cell cVarEq2 = variablesOf[eq2].right();

      while (cVarEq1 instanceof Data && cVarEq2 instanceof Data) {
         Data varEq1 = (Data) cVarEq1;
         Data varEq2 = (Data) cVarEq2;

         if (
               isBase[varEq1.variable]
                     || isBase[varEq2.variable]
                     || varEq1.variable == ignoreVar
                     || varEq2.variable == ignoreVar
         ) {
            if (isBase[varEq1.variable] || varEq1.variable == ignoreVar) {
               cVarEq1 = cVarEq1.right();
            }
            if (isBase[varEq2.variable] || varEq2.variable == ignoreVar) {
               cVarEq2 = cVarEq2.right();
            }
         } else {
            if (varEq1.variable != varEq2.variable) {
               break;
            }

            cVarEq1 = cVarEq1.right();
            cVarEq2 = cVarEq2.right();
         }
      }

      return (cVarEq1 instanceof Row && cVarEq2 instanceof Row)
            || (cVarEq1 instanceof Row && isBase[((Data) cVarEq2).variable])
            || (cVarEq2 instanceof Row && isBase[((Data) cVarEq1).variable]);
   }

   @Override
   public int baseVariableOf(int equation) {
      return baseOf[equation];
   }

   @Override
   public int baseVariableOf(Row equation) {
      return baseOf[equation.index];
   }

   @Override
   public Iterable<Row> activeEquations() {
      return root.rows();
   }

   @Override
   public Iterable<Column> activeVariables() {
      return root.columns();
   }

   @Override
   public boolean subsetOf(int subset, int superset) {
      Cell subsetCell = variablesOf[subset].right();
      Cell supersetCell = variablesOf[superset].right();

      while (subsetCell instanceof Data && supersetCell instanceof Data) {
         Data subsetVar = (Data) subsetCell;
         Data supersetVar = (Data) supersetCell;

         if (isBase[subsetVar.variable] || isBase[supersetVar.variable]) {
            if (isBase[subsetVar.variable]) {
               subsetCell = subsetCell.right();
            }
            if (isBase[supersetVar.variable]) {
               supersetCell = supersetCell.right();
            }
         } else {
            while (subsetVar.variable > supersetVar.variable) {
               supersetCell = supersetCell.right();
               if (supersetCell instanceof Data) {
                  supersetVar = (Data) supersetCell;
               } else {
                  return false;
               }
            }
            if (subsetVar.variable != supersetVar.variable) {
               break;
            }

            subsetCell = subsetCell.right();
            supersetCell = supersetCell.right();
         }
      }

      return subsetCell instanceof Row;
   }

   @Override
   public int firstOffBase(int pivot) {
      for (Data it : variablesOf[pivot]) {
         if (!isBase[it.variable]) {
            return it.variable;
         }
      }
      return -1;
   }

   @Override
   public int numberOfUndefinedVariables() {
      return numberOfUndefinedVariables;
   }
}

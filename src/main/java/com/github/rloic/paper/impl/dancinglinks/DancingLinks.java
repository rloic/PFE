package com.github.rloic.paper.impl.dancinglinks;

import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Function;
import java.util.function.Predicate;

@SuppressWarnings("unchecked")
public class DancingLinks<T> {

   private final T[] variables;
   private final Cell.Header<T>[] rows;
   private final Cell.Header<T>[] columns;
   private final Cell.Data<T>[][] cells;
   private final Function<T, String> debug;

   public DancingLinks(T[] variables, int[][] equations) {
      this(variables, equations, Object::toString);
   }

   public DancingLinks(T[] variables, int[][] equations, Function<T, String> debug) {
      this.variables = variables;
      rows = (Cell.Header<T>[]) Array.newInstance(Cell.Header.class, equations.length);
      rows[0] = new Cell.Header<>("row_0");
      for (int i = 1; i < equations.length; i++) {
         rows[i] = new Cell.Header<>("row_" + i);
         rows[i - 1].addBottom(rows[i]);
      }
      columns = (Cell.Header[]) Array.newInstance(Cell.Header.class, variables.length);
      columns[0] = new Cell.Header<>("col_0");
      for (int j = 1; j < variables.length; j++) {
         columns[j] = new Cell.Header<>("col_" + j);
         columns[j - 1].addRight(columns[j]);
      }
      cells = (Cell.Data[][]) Array.newInstance(Cell.Data.class, equations.length, variables.length);
      for (int i = 0; i < equations.length; i++) {
         for (int j = 0; j < equations[i].length; j++) {
            Arrays.sort(equations[i]);
            int variable = equations[i][j];
            Cell.Data<T> current = get(i, variable);
            Cell<T> lastElementOnCol = columns[variable].top();
            lastElementOnCol.addBottom(current);
            Cell<T> lastElementOnRow = rows[i].left();
            lastElementOnRow.addRight(current);
         }
      }
      this.debug = debug;
   }

   public Cell.Data<T> get(int x, int y) {
      if (cells[x][y] == null) {
         cells[x][y] = new Cell.Data<>(x, y, variables[y]);
      }
      return cells[x][y];
   }

   public Cell.Header<T> column(int y) {
      return columns[y];
   }

   public int nbColumns() {
      return columns.length;
   }

   public int nbRows() {
      return rows.length;
   }

   public Cell.Header<T> row(int x) {
      return rows[x];
   }

   public Iterable<Cell.Data<T>> columnIter(int column) {
      return () -> new Iterator<Cell.Data<T>>() {
         Cell cell = columns[column];
         @Override
         public boolean hasNext() {
            return cell.bottom() instanceof Cell.Data;
         }

         @Override
         public Cell.Data<T> next() {
            Cell.Data<T> result = (Cell.Data<T>) cell.bottom();
            cell = cell.bottom();
            return result;
         }
      };
   }

   public Iterable<Cell.Data<T>> rowIter(int row) {
      return () -> new Iterator<Cell.Data<T>>() {
         Cell cell = columns[row];
         @Override
         public boolean hasNext() {
            return cell.right() instanceof Cell.Data;
         }

         @Override
         public Cell.Data<T> next() {
            Cell.Data<T> result = (Cell.Data<T>) cell.right();
            cell = cell.right();
            return result;
         }
      };
   }

   public void removeRow(int x) {
      Cell<T> header = rows[x];
      Cell<T> cell = header;
      do {
         cell.top().unlinkBottom();
         cell = cell.right();
      } while (cell != header);
   }

   public void restoreRow(int x) {
      Cell<T> header = rows[x];
      Cell<T> cell = header;
      do {
         cell.top().addBottom(cell);
         cell = cell.right();
      } while (cell != header);
   }

   public void removeColumn(int y) {
      Cell<T> header = columns[y];
      Cell<T> cell = header;
      do {
         cell.left().unlinkRight();
         cell = cell.bottom();
      } while (cell != header);
   }

   public void restoreColumn(int y) {
      Cell<T> header = columns[y];
      Cell<T> cell = header;
      do {
         cell.left().addRight(cell);
         cell = cell.bottom();
      } while (cell != header);
   }

   public void xor(int target, int pivot) {
      Cell<T> headerT = rows[target];
      Cell<T> headerP = rows[pivot];

      Cell<T> cellT = headerT.right();
      Cell<T> cellP = headerP.right();

      while (cellT != headerT && cellP != headerP) {
         if (cellT.isOnTheRightOf(cellP)) {
            int y = ((Cell.Data) cellP).y;
            Cell.Data<T> newUnknown = get(target, y);
            Cell<T> top = findLastInColumn(y, it -> it.isAbove(newUnknown));
            Cell<T> left = cellT.left();
            linkCell(top, left, newUnknown);

            cellP = cellP.right();
         } else if (cellP.isOnTheRightOf(cellT)) {
            cellT = cellT.right();
         } else {
            cellT.left().unlinkRight();
            cellT.top().unlinkBottom();
            cellT = cellT.right();
            cellP = cellP.right();
         }
      }

      while (cellP != headerP) {
         int y = ((Cell.Data) cellP).y;
         Cell.Data<T> newUnknown = get(target, y);
         Cell<T> top = findLastInColumn(y, it -> it.isAbove(newUnknown));
         Cell<T> left = headerT.left();
         linkCell(top, left, newUnknown);

         cellP = cellP.right();
      }

   }

   public Cell<T> findLastInColumn(int y, Predicate<Cell.Data<T>> predicate) {
      Cell<T> header = columns[y];
      Cell<T> cell = header.top();
      while (cell instanceof Cell.Data && !predicate.test((Cell.Data<T>) cell)) {
         cell = cell.top();
      }
      return cell;
   }

   public boolean rowContains(int x, Predicate<Cell.Data<T>> predicate) {
      Cell<T> header = rows[x];
      Cell<T> cell = header.right();
      while (cell instanceof Cell.Data && !predicate.test((Cell.Data<T>) cell)) {
         cell = cell.right();
      }
      return cell != header;
   }



   private void linkCell(Cell<T> top, Cell<T> left, Cell<T> unknown) {
      top.addBottom(unknown);
      left.addRight(unknown);
   }

   @Override
   public String toString() {
      StringBuilder str = new StringBuilder("    ");
      for (int j = 0; j < columns.length; j++) {
         if (columns[j].isActive()) {
            if (columns[j].isSeed()) {
               str.append(" [s] ");
            } else {
               str.append(" [*] ");
            }
         } else {
            str.append(" [ ] ");
         }
      }
      str.append('\n');
      for (int i = 0; i < rows.length; i++) {
         if (rows[i].isActive()) {
            if (rows[i].isSeed()) {
               str.append("[s] ");
            } else {
               str.append("[*] ");
            }
         } else {
            str.append("[ ] ");
         }
         for (int j = 0; j < columns.length; j++) {
            if (cells[i][j] != null && cells[i][j].isActive()) {
               str.append(debugAndTrim(cells[i][j].value));
            } else {
               str.append("  _  ");
            }
         }
         str.append('\n');
      }
      return str.toString();
   }

   private String debugAndTrim(T value) {

      String debugText = debug.apply(value);
      if (debugText.length() < 5) {
         return padLeft(debugText);
      } else {
         return debugText.substring(0, 5);
      }

   }

   private String padLeft(String inputString) {
      if (inputString.length() >= 5) {
         return inputString;
      }
      StringBuilder sb = new StringBuilder();
      while (sb.length() < 5 - inputString.length()) {
         sb.append(' ');
      }
      sb.append(inputString);

      return sb.toString();
   }

}

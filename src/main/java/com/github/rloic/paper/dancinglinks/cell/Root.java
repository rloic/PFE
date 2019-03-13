package com.github.rloic.paper.dancinglinks.cell;

import java.util.Iterator;

final public class Root extends Cell {
   public Root() {
      top = this;
      left = this;
      right = this;
      bottom = this;
   }

   public Iterable<Row> rows() {
      return () -> new Iterator<Row>() {
         Cell cell = bottom;

         @Override
         public boolean hasNext() {
            return cell instanceof Row;
         }

         @Override
         public Row next() {
            Row row = (Row) cell;
            cell = cell.bottom;
            return row;
         }
      };
   }

   public Iterable<Column> columns() {
      return () -> new Iterator<Column>() {
         Cell cell = right;

         @Override
         public boolean hasNext() {
            return cell instanceof Column;
         }

         @Override
         public Column next() {
            Column column = (Column) cell;
            cell = cell.right;
            return column;
         }
      };
   }

   @Override
   public final boolean isActive() {
      return true;
   }

   @Override
   public final boolean isSeed() {
      return true;
   }
}

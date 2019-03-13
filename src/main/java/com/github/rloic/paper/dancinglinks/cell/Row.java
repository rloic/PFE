package com.github.rloic.paper.dancinglinks.cell;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

final public class Row extends Header {

   // checked
   private Row(Cell previous) {
      top = previous;
      bottom = previous.bottom;
      left = this;
      right = this;

      top.bottom = this;
      bottom.top = this;
   }

   public Row(Row previous) {
      this((Cell) previous);
   }

   public Row(Root previous) {
      this((Cell) previous);
   }

   //checked
   @Override
   public final void remove() {
      Cell cell = this;
      do {
         cell.top.bottom = cell.bottom;
         cell.bottom.top = cell.top;
         cell = cell.right;
      } while (cell != this);
   }

   // checked
   @Override
   public final void restore() {
      Cell cell = this;
      do {
         cell.top.bottom = cell;
         cell.bottom.top = cell;
         cell = cell.left;
      } while (cell != this);
   }

   @NotNull
   @Override
   public final Iterator<Data> iterator() {
      return new Iterator<Data>() {
         Cell cell = right;
         @Override
         public boolean hasNext() {
            return cell instanceof Data;
         }

         @Override
         public Data next() {
            Data data = (Data) cell;
            cell = cell.right;
            return data;
         }
      };
   }

   @Override
   public final boolean isActive() {
      return top.bottom == this
            && bottom.top != this;
   }

   @Override
   public final boolean isSeed() {
      return top instanceof Root && bottom instanceof Root;
   }
}

package com.github.rloic.paper.impl.dancinglinks.dancinglinks.cell;

import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

public class Column extends Header {

   private Column(Cell previous) {
      left = previous;
      right = previous.right;
      left.right = this;
      right.left = this;

      top = this;
      bottom = this;
   }

   public Column(Root previous) {
      this((Cell) previous);
   }

   public Column(Column previous) {
      this((Cell) previous);
   }

   @Override
   public void remove() {
      Cell cell = this;
      do {
         cell.left.right = cell.right;
         cell.right.left = cell.left;
         cell = cell.bottom;
      } while (cell != this);
   }

   @Override
   public void restore() {
      Cell cell = this;
      do {
         cell.left.right = cell;
         cell.right.left = cell;
         cell = cell.top;
      } while (cell != this);
   }

   @NotNull
   @Override
   public Iterator<Data> iterator() {
      return new Iterator<Data>() {
         Cell cell = bottom;

         @Override
         public boolean hasNext() {
            return cell instanceof Data;
         }

         @Override
         public Data next() {
            Data data = (Data) cell;
            cell = cell.bottom;
            return data;
         }
      };
   }

   @Override
   public boolean isActive() {
      return left.right == this
            && right.left == this;
   }

   @Override
   public boolean isSeed() {
      return left instanceof Root && right instanceof Root;
   }
}

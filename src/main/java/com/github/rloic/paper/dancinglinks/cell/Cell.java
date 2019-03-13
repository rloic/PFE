package com.github.rloic.paper.dancinglinks.cell;

public abstract class Cell {

   Cell top;
   Cell left;
   Cell right;
   Cell bottom;

   public Cell top() {
      return top;
   }

   public Cell left() {
      return left;
   }

   public Cell right() {
      return right;
   }

   public Cell bottom() {
      return bottom;
   }

   public abstract boolean isActive();
   public abstract boolean isSeed();

}

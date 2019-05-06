package com.github.rloic.paper.dancinglinks.cell;

/**
 * A Classic DancingLinks cell
 */
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

   /**
    * Indicates if the cell is active (i.e. The cells that are linked by the current cell link back to it)
    * @return true if the cell is active else false
    */
   public abstract boolean isActive();

   /**
    * Return is the cell is a seed (i.e. It the last cell)
    * @return true if the cell is a seed else false
    */
   public abstract boolean isSeed();

}

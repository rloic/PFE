package com.github.rloic.paper.impl;

import com.github.rloic.paper.impl.dancinglinks.Cell;

import java.util.Iterator;

public class RowIterator<T> implements Iterator<Cell.Data<T>> {

   private Cell<T> cell;

   public RowIterator(Cell<T> cell) {
      this.cell = cell;
   }

   @Override
   public boolean hasNext() {
      return !(cell.right() instanceof Cell.Header);
   }

   @Override
   public Cell.Data<T> next() {
      return (Cell.Data<T>) cell.right();
   }
}

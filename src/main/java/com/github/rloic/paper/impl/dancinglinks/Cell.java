package com.github.rloic.paper.impl.dancinglinks;

import java.util.Objects;

public abstract class Cell<T> {

   private Cell<T> left;
   private Cell<T> right;
   private Cell<T> top;
   private Cell<T> bottom;


   private Cell() {
      left = null;
      right = null;
      top = null;
      bottom = null;
   }

   abstract protected String label();

   public boolean isActive() {
      return left != null
            && left.right == this
            && right != null
            && right.left == this
            && top != null
            && top.bottom == this
            && bottom != null
            && bottom.top == this;
   }

   void addRight(Cell<T> cell) {
      Objects.requireNonNull(cell);
      right.left = cell;
      cell.right = right;
      right = cell;
      cell.left = this;
   }

   void addBottom(Cell<T> cell) {
      Objects.requireNonNull(cell);
      bottom.top = cell;
      cell.bottom = bottom;
      bottom = cell;
      cell.top = this;
   }

   void unlinkBottom() {
      bottom.bottom.top = this;
      bottom = bottom.bottom;
   }

   void unlinkRight() {
      right.right.left = this;
      right = right.right;
   }

   public Cell<T> left() {
      return left;
   }

   public Cell<T> right() {
      return right;
   }

   public Cell<T> bottom() {
      return bottom;
   }

   public Cell<T> top() {
      return top;
   }

   protected void left(Cell<T> left) {
      this.left = left;
   }

   protected void right(Cell<T> right) {
      this.right = right;
   }

   protected void top(Cell<T> top) {
      this.top = top;
   }

   protected void bottom(Cell<T> bottom) {
      this.bottom = bottom;
   }

   public abstract boolean isOnTheRightOf(Cell other);

   public abstract boolean isOnTheLeftOf(Cell other);

   public abstract boolean isUnder(Cell other);

   public abstract boolean isAbove(Cell other);

   @Override
   final public String toString() {
      /*
                               top.toShortString()
         left.toShortString()  toShortString()           addRight.toShortString()
                               addBottom.toShortString()
       */

      StringBuilder builder = new StringBuilder();
      builder.append("\t\t\t\t")
            .append(top != null ? top.label() : "-")
            .append("\n")
            .append(left != null ? left.label() : "-")
            .append("\t")
            .append(label())
            .append("\t")
            .append(right != null ? right.label() : "-")
            .append("\n")
            .append("\t\t\t\t")
            .append(bottom != null ? bottom.label() : "-");

      return builder.toString();
   }

   public static class Header<T> extends Cell<T> {

      private String name;

      public Header(String name) {
         super();
         this.name = name;
         left(this);
         right(this);
         top(this);
         bottom(this);
      }

      @Override
      public boolean isOnTheRightOf(Cell other) {
         return true;
      }

      @Override
      public boolean isOnTheLeftOf(Cell other) {
         return true;
      }

      @Override
      public boolean isUnder(Cell other) {
         return true;
      }

      @Override
      public boolean isAbove(Cell other) {
         return true;
      }

      boolean isSeed() {
         if (right() instanceof Cell.Data) {
            return top() == this && bottom() == this;
         } else if (bottom() instanceof Cell.Data) {
            return left() == this && right() == this;
         }
         return left() == this && right() == this && top() == this && bottom() == this;
      }

      @Override
      protected String label() {
         return "Header(" + name + ")";
      }
   }

   public static class Data<T> extends Cell<T> {
      public final int x;
      public final int y;
      public final T value;

      public Data(int x, int y, T value) {
         super();
         this.x = x;
         this.y = y;
         this.value = value;
      }

      @Override
      public boolean isOnTheRightOf(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return y > that.y;
      }

      @Override
      public boolean isOnTheLeftOf(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return y < that.y;
      }

      @Override
      public boolean isUnder(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return x > that.x;
      }

      @Override
      public boolean isAbove(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return x < that.x;
      }

      @Override
      protected String label() {
         return "Data(value=" + value + ")";
      }
   }

}

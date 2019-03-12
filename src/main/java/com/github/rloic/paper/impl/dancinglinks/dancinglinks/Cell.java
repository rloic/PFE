package com.github.rloic.paper.impl.dancinglinks.dancinglinks;

import java.util.Objects;

public abstract class Cell {

   private Cell left;
   private Cell right;
   private Cell top;
   private Cell bottom;


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

   public void addRight(Cell cell) {
      Objects.requireNonNull(cell);
      right.left = cell;
      cell.right = right;
      right = cell;
      cell.left = this;
   }

   public void addBottom(Cell cell) {
      Objects.requireNonNull(cell);
      bottom.top = cell;
      cell.bottom = bottom;
      bottom = cell;
      cell.top = this;
   }

   public void unlinkBottom() {
      bottom.bottom.top = this;
      bottom = bottom.bottom;
   }

   public void unlinkRight() {
      right.right.left = this;
      right = right.right;
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

   public Cell top() {
      return top;
   }

   protected void left(Cell left) {
      this.left = left;
   }

   protected void right(Cell right) {
      this.right = right;
   }

   protected void top(Cell top) {
      this.top = top;
   }

   protected void bottom(Cell bottom) {
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

   public static class Header extends Cell {

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

   public static class Data extends Cell {
      public final int equation;
      public final int variable;

      public Data(int equation, int variable) {
         super();
         this.equation = equation;
         this.variable = variable;
      }

      @Override
      public boolean isOnTheRightOf(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return variable > that.variable;
      }

      @Override
      public boolean isOnTheLeftOf(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return variable < that.variable;
      }

      @Override
      public boolean isUnder(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return equation > that.equation;
      }

      @Override
      public boolean isAbove(Cell other) {
         if (other instanceof Header) {
            return true;
         }
         Cell.Data that = (Cell.Data) other;
         return equation < that.equation;
      }

      @Override
      protected String label() {
         return "Data(equation=" + equation + ", " + variable + ")";
      }
   }

}

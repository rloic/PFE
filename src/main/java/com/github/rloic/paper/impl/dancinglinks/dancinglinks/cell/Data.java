package com.github.rloic.paper.impl.dancinglinks.dancinglinks.cell;

public class Data extends Cell implements Removable, Restorable {

   public final int equation;
   public final int variable;

   public Data(
         int equation,
         int variable
   ) {
      this.equation = equation;
      this.variable = variable;
      top = this;
      left = this;
      right = this;
      bottom = this;
   }


   public Data(int equation, int variable, Row left, Column top) {
      this(equation, variable, (Cell) left, (Cell) top);
   }

   public Data(int equation, int variable, Data left, Column top) {
      this(equation, variable, (Cell) left, (Cell) top);
   }

   public Data(int equation, int variable, Row left, Data top) {
      this(equation, variable, (Cell) left, (Cell) top);
   }

   public Data(int equation, int variable, Data left, Data top) {
      this(equation, variable, (Cell) left, (Cell) top);
   }


   private Data(
         int equation,
         int variable,
         Cell left,
         Cell top
   ) {
      this.equation = equation;
      this.variable = variable;
      this.top = top;
      this.left = left;
      this.right = this.left.right;
      this.bottom = this.top.bottom;

      this.left.right = this;
      this.right.left = this;
      this.top.bottom = this;
      this.bottom.top = this;
   }

   @Override
   public void remove() {
      left.right = right;
      right.left = left;
      top.bottom = bottom;
      bottom.top = top;
   }

   @Override
   public void restore() {
      left.right = this;
      right.left = this;
      top.bottom = this;
      bottom.top = this;
   }

   public void relink(Cell left, Cell top) {
      this.left = left;
      right = left.right;
      left.right = this;
      right.left = this;

      this.top = top;
      bottom = top.bottom;
      top.bottom = this;
      bottom.top = this;
   }

   public boolean isOnTheRightOf(Data other) {
      return variable > other.variable;
   }

   public boolean isAboveOf(Data other) {
      return equation < other.equation;
   }

   @Override
   public boolean isActive() {
      return left != this && left.right == this
            && right != this && right.left == this
            && top != this && top.bottom == this
            && bottom != this && bottom.top == this;
   }

   @Override
   public boolean isSeed() {
      return false;
   }
}

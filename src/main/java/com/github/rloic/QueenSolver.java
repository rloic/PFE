package com.github.rloic;

import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.IntVar;


public class QueenSolver {

   public static void main(String[] args) {

      int n = 8;
      Model m = new Model();

      IntVar[] DX = new IntVar[n];
      for (int col = 0; col < n; col++) {
         DX[col] = m.intVar("Queen[" + col + "]", 0, n - 1);
      }

      for (int i = 0; i < n - 1; i++) {
         for (int j = i + 1; j < n; j++) {
            DX[i].ne(DX[j]).post();
            DX[i].ne(DX[j].min(j-i)).post();
            DX[i].ne(DX[j].min(i-j)).post();
         }
      }

      System.out.println(m.getSolver().findSolution());

   }

}

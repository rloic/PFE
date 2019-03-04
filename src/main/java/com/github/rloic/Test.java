package com.github.rloic;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.Algorithms;
import com.github.rloic.paper.XORMatrix;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import org.chocosolver.solver.exception.ContradictionException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Test {

   public static void main(String[] args) throws IOException, ContradictionException {
      int A = 0;
      int B = 1;
      int C = 2;
      int D = 3;
      int E = 4;
      int F = 5;
      int G = 6;

      XORMatrix m = new NaiveMatrixImpl(new int[][]{
            new int[]{A, B, D},
            new int[]{C, D, E},
      }, 5);

      List<Affectation> affectations = new ArrayList<>();
      Algorithms.normalize(m, affectations);
   }

}

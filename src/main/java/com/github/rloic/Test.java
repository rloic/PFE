package com.github.rloic;

import com.github.rloic.paper.impl.dancinglinks.Cell;
import com.github.rloic.paper.impl.dancinglinks.DancingLinks;
import com.github.rloic.paper.impl.dancinglinks.DancingLinksMatrix;
import com.github.rloic.paper.impl.dancinglinks.actions.PivotElection;
import com.github.rloic.paper.impl.dancinglinks.actions.XOR;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.chocosolver.solver.ICause;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.exception.ContradictionException;
import org.chocosolver.solver.learn.ExplanationForSignedClause;
import org.chocosolver.solver.learn.Implications;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.util.objects.ValueSortedMap;

import java.io.IOException;
import java.util.Arrays;

public class Test {

   private static final int A = 0;
   private static final int B = 1;
   private static final int C = 2;
   private static final int D = 3;
   private static final int E = 4;
   private static final int F = 5;
   private static final int G = 6;

   private static String debug(BoolVar v) {
      if (v.isInstantiated()) {
         return v.getValue() == 1 ? "  1  " : "  0  ";
      } else {
         return "  x  ";
      }
   }

   final static ICause because = new ICause() {
      @Override
      public void explain(ExplanationForSignedClause explanation, ValueSortedMap<IntVar> front, Implications implicationGraph, int pivot) {

      }
   };

   public static void main(String[] args) throws IOException, ContradictionException {
      Model m = new Model();

      BoolVar[] variables = m.boolVarArray(7);

      DancingLinks<BoolVar> matrix = new DancingLinks<>(
            variables,
            new int[][]{
                  new int[]{C, E, F},
                  new int[]{D, F, G},
                  new int[]{A, B, C},
                  new int[]{B, C, E}
            },
            Test::debug
      );
   }

   void gauss(
         DancingLinks<BoolVar> matrix,
         boolean[] isBase,
         int[] pivotOf
   ) {

      boolean[] isPivot = new boolean[matrix.nbRows()];
      boolean[] hadAOne = new boolean[matrix.nbRows()];
      Arrays.fill(pivotOf, -1);

   

   }



}

package com.github.rloic.paper;

import com.github.rloic.inference.impl.Affectation;
import com.github.rloic.paper.impl.NaiveMatrixImpl;
import com.github.rloic.util.Logger;
import com.github.rloic.xorconstraint.GlobalXorPropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solver;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.rloic.util.Logger.InfoLogger.INFO;
import static com.github.rloic.util.Logger.TraceLogger.TRACE;

public class Algorithms {

   public static void main(String[] args) {
      Logger.level(INFO);

      Model m = new Model();
      BoolVar[] variables = m.boolVarArray(5);
      m.post(new Constraint("GlobalC", new GlobalXorPropagator(variables, new BoolVar[][]{
            new BoolVar[]{variables[0], variables[1], variables[2]},
            new BoolVar[]{variables[2], variables[3], variables[4]}
      })));

      Solver s = m.getSolver();
      while (s.solve()) {
         System.out.println(Arrays.stream(variables).map(IntVar::getValue).collect(Collectors.toList()));
      }
      s.printShortStatistics();

      Model m2 = new Model();
      BoolVar[] variables2 = m2.boolVarArray(4);
      m2.post(new Constraint("GlobalC", new GlobalXorPropagator(variables2, new BoolVar[][]{
            new BoolVar[]{variables2[0], variables2[2], variables2[3]},
            new BoolVar[]{variables2[1], variables2[2], variables2[3]}
      })));

      Solver s2 = m2.getSolver();
      while (s2.solve()) {
         System.out.println(Arrays.stream(variables2).map(IntVar::getValue).collect(Collectors.toList()));
      }
      s2.printShortStatistics();

   }

   static boolean makePivot(XORMatrix m, int pivot, int variable, List<Affectation> F) {
      Logger.trace("Make pivot: pivot=" + pivot + ", base_var=" + variable);
      Logger.trace("\n" + m);
      if (m.nbTrues(pivot) == 0 && m.nbUnknowns(pivot) == 1) {
         assert m.firstUndefined(pivot) != -1;
         F.add(new Affectation(m.firstUndefined(pivot), false));
      }
      if (m.nbTrues(pivot) == 1 && m.nbUnknowns(pivot) == 1) {
         assert m.firstUndefined(pivot) != -1;
         F.add(new Affectation(m.firstUndefined(pivot), true));
      }
      for (int k : m.rows()) {
         if (k != pivot && (m.isUndefined(k, variable) || m.isTrue(k, variable))) {
            boolean isValid = m.xor(k, pivot);
            if(!isValid) return false;
            if (m.nbTrues(k) + m.nbUnknowns(k) <= 1) {
               Logger.warn("After xor line: m.nbTrues(k) + m.nbUnknowns(k) = " + (m.nbTrues(k) + m.nbUnknowns(k)));
            }
            if (m.nbTrues(k) == 1 && m.nbUnknowns(k) == 0) {
               return false;
            }
            if (m.nbTrues(k) == 0 && m.nbUnknowns(k) == 1) {
               assert m.firstUndefined(k) != -1;
               F.add(new Affectation(m.firstUndefined(k), false));
            }
            if (m.nbTrues(k) == 1 && m.nbUnknowns(k) == 1) {
               assert m.firstUndefined(k) != -1;
               F.add(new Affectation(m.firstUndefined(k), true));
            }
         }
      }
      m.setBase(pivot, variable);
      return true;
   }

   public static boolean normalize(XORMatrix m, List<Affectation> F) {
      int base = 0;
      int i = base, j = base;
      while (base < m.nbRows() && j < m.nbColumns()) {
         if (m.isUndefined(i, j) || m.isTrue(i, j)) {
            if (i != base) m.swap(i, base);
            if (!makePivot(m, base, j, F)) return false;
            base += 1;
            i = base;
            j = base;
         } else {
            if (i == m.nbRows() - 1) {
               i = base;
               j += 1;
            } else {
               i += 1;
            }
         }
      }
      for (int row : m.rows()) {
         if (m.nbTrues(row) == 1 && m.nbUnknowns(row) == 0) return false;
      }
      return true;
   }

}

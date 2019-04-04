package com.github.rloic.midori;

import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.wip.PhantomDiffSum;
import com.github.rloic.xorconstraint.BasePropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MidoriGlobalFull {

   public final Model m;
   public final BoolVar[] sBoxes;
   public final BoolVar[] assignedVar;
   public final BasePropagator propagator;

   private List<BoolVar> xorElements = new ArrayList<>();
   private List<BoolVar[]> xorEquations = new ArrayList<>();

   private final List<Set<BoolVar>> columns = new ArrayList<>();
   private final List<Set<BoolVar>> rows = new ArrayList<>();
   private final List<Set<BoolVar>> rounds = new ArrayList<>();

   public MidoriGlobalFull(int r, int objStep1) {
      m = new Model("Midori Global[1-5]");
      BoolVar[][][] DX = new BoolVar[r][4][4];
      BoolVar[][][] DY = new BoolVar[r - 1][4][4];
      BoolVar[][][] DZ = new BoolVar[r - 1][4][4];
      BoolVar[][] DK = m.boolVarMatrix("DK", 4, 4);

      for (int i = 0; i < r; i++) {
         rounds.add(new HashSet<>());
      }
      for (int j = 0; j < 4; j++) {
         rows.add(new HashSet<>());
      }
      for (int k = 0; k < 4; k++) {
         columns.add(new HashSet<>());
      }

      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               DX[i][j][k] = m.boolVar("DX[" + i + "][" + j + "][" + k + "]");
               dispatch(i, j, k, DX[i][j][k]);
               DZ[i][j][k] = m.boolVar("DZ[" + i + "][" + j + "][" + k + "]");
               dispatch(i, j, k, DZ[i][j][k]);
            }
         }
      }
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            DX[(r - 1)][j][k] = m.boolVar("DX[" + (r - 1) + "][" + j + "][" + k + "]");
            dispatch((r - 1), j, k, DX[(r - 1)][j][k]);
         }
      }

      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               xor(DZ[i][j][k], DK[j][k], DX[i + 1][j][k]);
            }
         }
      }

      for (int i = 0; i < r - 1; i++) {
         DY[i][0][0] = m.boolVar();
         m.arithm(DY[i][0][0], "=", DX[i][0][0]).post();
         dispatch(i, 0, 0, DY[i][0][0]);
         DY[i][1][0] = m.boolVar();
         m.arithm(DY[i][1][0], "=", DX[i][2][2]).post();
         dispatch(i, 1, 0, DY[i][1][0]);
         DY[i][2][0] = m.boolVar();
         m.arithm(DY[i][2][0], "=", DX[i][1][1]).post();
         dispatch(i, 2, 0, DY[i][2][0]);
         DY[i][3][0] = m.boolVar();
         m.arithm(DY[i][3][0], "=", DX[i][3][3]).post();
         dispatch(i, 3, 0, DY[i][3][0]);

         DY[i][0][1] = m.boolVar();
         m.arithm(DY[i][0][1], "=", DX[i][2][3]).post();
         dispatch(i, 0, 1, DY[i][0][1]);
         DY[i][1][1] = m.boolVar();
         m.arithm(DY[i][1][1], "=", DX[i][0][1]).post();
         dispatch(i, 1, 1, DY[i][1][1]);
         DY[i][2][1] = m.boolVar();
         m.arithm(DY[i][2][1], "=", DX[i][3][2]).post();
         dispatch(i, 2, 1, DY[i][2][1]);
         DY[i][3][1] = m.boolVar();
         m.arithm(DY[i][3][1], "=", DX[i][1][0]).post();
         dispatch(i, 3, 1, DY[i][3][1]);

         DY[i][0][2] = m.boolVar();
         m.arithm(DY[i][0][2], "=", DX[i][1][2]).post();
         dispatch(i, 0, 2, DY[i][0][2]);
         DY[i][1][2] = m.boolVar();
         m.arithm(DY[i][1][2], "=", DX[i][3][0]).post();
         dispatch(i, 1, 2, DY[i][1][2]);
         DY[i][2][2] = m.boolVar();
         m.arithm(DY[i][2][2], "=", DX[i][0][3]).post();
         dispatch(i, 2, 2, DY[i][2][2]);
         DY[i][3][2] = m.boolVar();
         m.arithm(DY[i][3][2], "=", DX[i][2][1]).post();
         dispatch(i, 3, 2, DY[i][3][2]);

         DY[i][0][3] = m.boolVar();
         m.arithm(DY[i][0][3], "=", DX[i][3][1]).post();
         dispatch(i, 0, 3, DY[i][0][3]);
         DY[i][1][3] = m.boolVar();
         m.arithm(DY[i][1][3], "=", DX[i][1][3]).post();
         dispatch(i, 1, 3, DY[i][1][3]);
         DY[i][2][3] = m.boolVar();
         m.arithm(DY[i][2][3], "=", DX[i][2][0]).post();
         dispatch(i, 2, 3, DY[i][2][3]);
         DY[i][3][3] = m.boolVar();
         m.arithm(DY[i][3][3], "=", DX[i][0][2]).post();
         dispatch(i, 3, 3, DY[i][3][3]);
      }

      for (int i = 0; i < r - 1; i++) {
         for (int k = 0; k < 4; k++) {
            xor(DY[i][1][k], DY[i][2][k], DY[i][3][k], DZ[i][0][k]);
            xor(DY[i][0][k], DY[i][2][k], DY[i][3][k], DZ[i][1][k]);
            xor(DY[i][0][k], DY[i][1][k], DY[i][3][k], DZ[i][2][k]);
            xor(DY[i][0][k], DY[i][1][k], DY[i][2][k], DZ[i][3][k]);
         }
      }

      sBoxes = new BoolVar[r * 4 * 4];
      int cpt = 0;
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               sBoxes[cpt++] = DX[i][j][k];
            }
         }
      }

      m.sum(sBoxes, "=", objStep1).post();

      BoolVar[] vars = new BoolVar[xorElements.size()];
      xorElements.toArray(vars);

      BoolVar[][] xors = new BoolVar[xorEquations.size()][];
      xorEquations.toArray(xors);

      propagator = new BasePropagator(
            vars,
            xors,
            columns,
            rows,
            rounds,
            new FullInferenceEngine(),
            new FullRulesApplier(),
            m.getSolver()
      );
      m.post(new Constraint("Global XOR", propagator));

      assignedVar = new BoolVar[r * 4 * 4];
      cpt = 0;
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j <= 3; j++) {
            for (int k = 0; k <= 3; k++) {
               assignedVar[cpt++] = DZ[i][j][k];
            }
         }
      }
      for (int j = 0; j <= 3; j++) {
         for (int k = 0; k <= 3; k++) {
            assignedVar[cpt++] = DK[j][k];
         }
      }

   }

   private void dispatch(int i, int j, int k, BoolVar variable) {
      rounds.get(i).add(variable);
      rows.get(j).add(variable);
      columns.get(k).add(variable);
   }

   private void xor(BoolVar... equation) {
      for (BoolVar var : equation) {
         if (!xorElements.contains(var)) {
            xorElements.add(var);
         }
      }
      xorEquations.add(equation);
      m.post(new Constraint("Phantom Sum", new PhantomDiffSum(1, equation)));
   }
}
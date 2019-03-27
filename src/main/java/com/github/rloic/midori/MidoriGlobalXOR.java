package com.github.rloic.midori;

import com.github.rloic.xorconstraint.BasePropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

public class MidoriGlobalXOR {

   public final Model m;
   public final BoolVar[] sBoxes;
   public final BoolVar[] assignedVar;

   private List<BoolVar> xorElements = new ArrayList<>();
   private List<BoolVar[]> xorEquations = new ArrayList<>();


   public MidoriGlobalXOR(int r, int objStep1) {
      m = new Model("Midori Global XOR");
      BoolVar[][][] DX = new BoolVar[r][][];
      BoolVar[][][] DY = new BoolVar[r - 1][4][4];
      BoolVar[][][] DZ = new BoolVar[r - 1][][];
      BoolVar[][] DK = m.boolVarMatrix("DK", 4, 4);

      for (int i = 0; i < r - 1; i++) {
         DX[i] = m.boolVarMatrix("DX[" + i + "]", 4, 4);
         DZ[i] = m.boolVarMatrix("DZ[" + i + "]", 4, 4);
      }
      DX[r - 1] = m.boolVarMatrix("DX[" + (r - 1) + "]", 4, 4);

      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               xor(DZ[i][j][k], DK[j][k], DX[i + 1][j][k]);
            }
         }
      }

      for (int i = 0; i < r - 1; i++) {
         DY[i][0][0] = DX[i][0][0];
         DY[i][1][0] = DX[i][2][2];
         DY[i][2][0] = DX[i][1][1];
         DY[i][3][0] = DX[i][3][3];

         DY[i][0][1] = DX[i][2][3];
         DY[i][1][1] = DX[i][0][1];
         DY[i][2][1] = DX[i][3][2];
         DY[i][3][1] = DX[i][1][0];

         DY[i][0][2] = DX[i][1][2];
         DY[i][1][2] = DX[i][3][0];
         DY[i][2][2] = DX[i][0][3];
         DY[i][3][2] = DX[i][2][1];

         DY[i][0][3] = DX[i][3][1];
         DY[i][1][3] = DX[i][1][3];
         DY[i][2][3] = DX[i][2][0];
         DY[i][3][3] = DX[i][0][2];
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

      BasePropagator globalXORProp = new BasePropagator(vars, xors, m.getSolver());
      m.post(new Constraint("Global XOR", globalXORProp));

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

   private void xor(BoolVar... equation) {
      for (BoolVar var : equation) {
         if (!xorElements.contains(var)) {
            xorElements.add(var);
         }
      }
      xorEquations.add(equation);
      IntVar[] variables = new IntVar[equation.length];
      System.arraycopy(equation, 0, variables, 0, equation.length);
      m.sum(variables, "!=", 1).post();
   }
}

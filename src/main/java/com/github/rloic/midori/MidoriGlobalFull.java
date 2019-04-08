package com.github.rloic.midori;

import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.BasePropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.function.Predicate;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;

public class MidoriGlobalFull {

   public final Model m;
   public final BoolVar[] sBoxes;
   public final BoolVar[] assignedVar;
   public final BasePropagator propagator;

   private List<BoolVar> xorElements = new ArrayList<>();
   private List<BoolVar[]> xorEquations = new ArrayList<>();

   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   public MidoriGlobalFull(int r, int objStep1) {
      m = new Model("Midori Global[1-5]");

      this.constraintsOf = new Int2ObjectArrayMap<>();

      BoolVar[][][] DX = new BoolVar[r][4][4];
      BoolVar[][][] DY = new BoolVar[r - 1][4][4];
      BoolVar[][][] DZ = new BoolVar[r - 1][4][4];
      BoolVar[][] DK = m.boolVarMatrix("DK", 4, 4);
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            constraintsOf.put(DK[j][k].getId(), new ArrayList<>());
         }
      }

      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               DX[i][j][k] = m.boolVar("DX[" + i + "][" + j + "][" + k + "]");
               constraintsOf.put(DX[i][j][k].getId(), new ArrayList<>());
               DZ[i][j][k] = m.boolVar("DZ[" + i + "][" + j + "][" + k + "]");
               constraintsOf.put(DZ[i][j][k].getId(), new ArrayList<>());
            }
         }
      }
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            DX[(r - 1)][j][k] = m.boolVar("DX[" + (r - 1) + "][" + j + "][" + k + "]");
            constraintsOf.put(DX[(r - 1)][j][k].getId(), new ArrayList<>());
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
         constraintsOf.put(DY[i][0][0].getId(), new ArrayList<>());
         equals(DY[i][0][0], DX[i][0][0]);
         DY[i][1][0] = m.boolVar();
         constraintsOf.put(DY[i][1][0].getId(), new ArrayList<>());
         equals(DY[i][1][0], DX[i][2][2]);
         DY[i][2][0] = m.boolVar();
         constraintsOf.put(DY[i][2][0].getId(), new ArrayList<>());
         equals(DY[i][2][0], DX[i][1][1]);
         DY[i][3][0] = m.boolVar();
         constraintsOf.put(DY[i][3][0].getId(), new ArrayList<>());
         equals(DY[i][3][0], DX[i][3][3]);
         DY[i][0][1] = m.boolVar();
         constraintsOf.put(DY[i][0][1].getId(), new ArrayList<>());
         equals(DY[i][0][1], DX[i][2][3]);
         DY[i][1][1] = m.boolVar();
         constraintsOf.put(DY[i][1][1].getId(), new ArrayList<>());
         equals(DY[i][1][1], DX[i][0][1]);
         DY[i][2][1] = m.boolVar();
         constraintsOf.put(DY[i][2][1].getId(), new ArrayList<>());
         equals(DY[i][2][1], DX[i][3][2]);
         DY[i][3][1] = m.boolVar();
         constraintsOf.put(DY[i][3][1].getId(), new ArrayList<>());
         equals(DY[i][3][1], DX[i][1][0]);
         DY[i][0][2] = m.boolVar();
         constraintsOf.put(DY[i][0][2].getId(), new ArrayList<>());
         equals(DY[i][0][2], DX[i][1][2]);
         DY[i][1][2] = m.boolVar();
         constraintsOf.put(DY[i][1][2].getId(), new ArrayList<>());
         equals(DY[i][1][2], DX[i][3][0]);
         DY[i][2][2] = m.boolVar();
         constraintsOf.put(DY[i][2][2].getId(), new ArrayList<>());
         equals(DY[i][2][2], DX[i][0][3]);
         DY[i][3][2] = m.boolVar();
         constraintsOf.put(DY[i][3][2].getId(), new ArrayList<>());
         equals(DY[i][3][2], DX[i][2][1]);
         DY[i][0][3] = m.boolVar();
         constraintsOf.put(DY[i][0][3].getId(), new ArrayList<>());
         equals(DY[i][0][3], DX[i][3][1]);
         DY[i][1][3] = m.boolVar();
         constraintsOf.put(DY[i][1][3].getId(), new ArrayList<>());
         equals(DY[i][1][3], DX[i][1][3]);
         DY[i][2][3] = m.boolVar();
         constraintsOf.put(DY[i][2][3].getId(), new ArrayList<>());
         equals(DY[i][2][3], DX[i][2][0]);
         DY[i][3][3] = m.boolVar();
         constraintsOf.put(DY[i][3][3].getId(), new ArrayList<>());
         equals(DY[i][3][3], DX[i][0][2]);

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
            new FullInferenceEngine(),
            new FullRulesApplier(),
            m.getSolver()
      );
      m.post(new Constraint("Global XOR", propagator));

      List<BoolVar> assignedVarList = new ArrayList<>();
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j <= 3; j++) {
            for (int k = 0; k <= 3; k++) {
               assignedVarList.add(DZ[i][j][k]);
               assignedVarList.add(DY[i][j][k]);
            }
         }
      }
      for (int j = 0; j <= 3; j++) {
         for (int k = 0; k <= 3; k++) {
            assignedVarList.add(DK[j][k]);
         }
      }

      assignedVar = new BoolVar[assignedVarList.size()];
      assignedVarList.toArray(assignedVar);

   }

   private void xor(BoolVar... equation) {
      WeightedConstraint wXor = new WeightedConstraint(
            equation,
            (vars) -> Arrays.stream(vars).mapToInt(IntVar::getValue).sum() == 1
      );

      for (int i = 0; i < equation.length; i++) {
         BoolVar var = equation[i];
         if (!xorElements.contains(var)) {
            xorElements.add(var);
         }
         constraintsOf.get(var.getId()).add(wXor);
      }

      xorEquations.add(equation);
   }

   private void equals(BoolVar A, BoolVar B) {
      WeightedConstraint equals = new WeightedConstraint(
            arrayOf(A, B),
            (variables) -> Arrays.stream(variables)
                  .allMatch(it -> it.getValue() == variables[0].getValue())
      );
      m.arithm(A, "=", B).post();
      constraintsOf.get(A.getId()).add(equals);
      constraintsOf.get(B.getId()).add(equals);
   }

}
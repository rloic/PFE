package com.github.rloic.midori;

import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.paper.dancinglinks.inferenceengine.InferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.RulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.BasePropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;

import java.util.List;

@SuppressWarnings("NonAsciiCharacters")
public abstract class MidoriGlobal {

   public final Model m;
   public final BoolVar[] sBoxes;
   public final BoolVar[] variablesToAssign;
   public final BasePropagator propagator;
   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   private final ExtendedModel em;
   private int sBoxesInc = 0;
   private int variablesToAssignedInc = 0;

   abstract protected String getModelName();

   abstract protected InferenceEngine getInferenceEngine();

   abstract protected RulesApplier getRulesApplier();

   MidoriGlobal(int r, int objStep1) {
      em = new ExtendedModel(getModelName());

      BoolVar[][][] ΔX = new BoolVar[r][4][4];
      BoolVar[][][] ΔZ = new BoolVar[r - 1][4][4];
      BoolVar[][] ΔK = new BoolVar[4][4];
      BoolVar[][][] ΔY = new BoolVar[r - 1][4][4];

      sBoxes = new BoolVar[r * 4 * 4];
      variablesToAssign = new BoolVar[(2 * r - 1) * 4 * 4];

      // Init ΔX
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               ΔX[i][j][k] = createSBox("ΔX[" + i + "][" + j + "][" + k + "]");
            }
         }
      }

      // Init ΔZ
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               ΔZ[i][j][k] = createVariableToAssign("ΔZ[" + i + "][" + j + "][" + k + "]");

            }
         }
      }

      // Init ΔK
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            ΔK[j][k] = createVariableToAssign("ΔK[" + j + "][" + k + "]");
         }
      }

      // Init ΔSX alias ΔY
      for (int i = 0; i < r - 1; i++) {
         ΔY[i][0][0] = createVariableToAssign("ΔY/ΔSX[i][0][0]");
         em.equals(ΔY[i][0][0], ΔX[i][0][0]);
         ΔY[i][1][0] = createVariableToAssign("ΔY/ΔSX[i][1][0]");
         em.equals(ΔY[i][1][0], ΔX[i][2][2]);
         ΔY[i][2][0] = createVariableToAssign("ΔY/ΔSX[i][2][0]");
         em.equals(ΔY[i][2][0], ΔX[i][1][1]);
         ΔY[i][3][0] = createVariableToAssign("ΔY/ΔSX[i][3][0]");
         em.equals(ΔY[i][3][0], ΔX[i][3][3]);
         ΔY[i][0][1] = createVariableToAssign("ΔY/ΔSX[i][0][1]");
         em.equals(ΔY[i][0][1], ΔX[i][2][3]);
         ΔY[i][1][1] = createVariableToAssign("ΔY/ΔSX[i][1][1]");
         em.equals(ΔY[i][1][1], ΔX[i][0][1]);
         ΔY[i][2][1] = createVariableToAssign("ΔY/ΔSX[i][2][1]");
         em.equals(ΔY[i][2][1], ΔX[i][3][2]);
         ΔY[i][3][1] = createVariableToAssign("ΔY/ΔSX[i][3][1]");
         em.equals(ΔY[i][3][1], ΔX[i][1][0]);
         ΔY[i][0][2] = createVariableToAssign("ΔY/ΔSX[i][0][2]");
         em.equals(ΔY[i][0][2], ΔX[i][1][2]);
         ΔY[i][1][2] = createVariableToAssign("ΔY/ΔSX[i][1][2]");
         em.equals(ΔY[i][1][2], ΔX[i][3][0]);
         ΔY[i][2][2] = createVariableToAssign("ΔY/ΔSX[i][2][2]");
         em.equals(ΔY[i][2][2], ΔX[i][0][3]);
         ΔY[i][3][2] = createVariableToAssign("ΔY/ΔSX[i][3][2]");
         em.equals(ΔY[i][3][2], ΔX[i][2][1]);
         ΔY[i][0][3] = createVariableToAssign("ΔY/ΔSX[i][0][3]");
         em.equals(ΔY[i][0][3], ΔX[i][3][1]);
         ΔY[i][1][3] = createVariableToAssign("ΔY/ΔSX[i][1][3]");
         em.equals(ΔY[i][1][3], ΔX[i][1][3]);
         ΔY[i][2][3] = createVariableToAssign("ΔY/ΔSX[i][2][3]");
         em.equals(ΔY[i][2][3], ΔX[i][2][0]);
         ΔY[i][3][3] = createVariableToAssign("ΔY/ΔSX[i][3][3]");
         em.equals(ΔY[i][3][3], ΔX[i][0][2]);
      }

      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               em.abstractXor(ΔZ[i][j][k], ΔK[j][k], ΔX[i + 1][j][k]);
            }
         }
      }

      for (int i = 0; i < r - 1; i++) {
         for (int k = 0; k < 4; k++) {
            em.abstractXor(ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k], ΔZ[i][0][k]);
            em.abstractXor(ΔY[i][0][k], ΔY[i][2][k], ΔY[i][3][k], ΔZ[i][1][k]);
            em.abstractXor(ΔY[i][0][k], ΔY[i][1][k], ΔY[i][3][k], ΔZ[i][2][k]);
            em.abstractXor(ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔZ[i][3][k]);
         }
      }

      em.sum(sBoxes, "=", objStep1);

      DeconstructedModel dm = em.build(getInferenceEngine(), getRulesApplier());
      this.m = dm.model;
      this.propagator = dm.propagator;
      this.constraintsOf = dm.constraintsOf;
   }

   private BoolVar createSBox(String name) {
      BoolVar var = em.boolVar(name);
      sBoxes[sBoxesInc++] = var;
      return var;
   }

   private BoolVar createVariableToAssign(String name) {
      BoolVar var = em.boolVar(name);
      variablesToAssign[variablesToAssignedInc++] = var;
      return var;
   }

}

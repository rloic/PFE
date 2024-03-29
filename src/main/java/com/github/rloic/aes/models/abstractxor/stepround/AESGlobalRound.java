package com.github.rloic.aes.models.abstractxor.stepround;

import com.github.rloic.aes.utils.KeyBits;
import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.common.collections.BytePosition;
import com.github.rloic.constraints.abstractxor.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.constraints.abstractxor.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.common.utils.Pair;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.constraints.abstractxor.AbstractXORPropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

import static com.github.rloic.aes.utils.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.utils.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.utils.KeyBits.AES256.AES_256;
import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.common.collections.ArrayExtensions.intArrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class AESGlobalRound {

   private final ExtendedModel em;
   private final KeyBits keyBits;

   public final Model m;
   public final AbstractXORPropagator propagator;
   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   public final BoolVar[] sBoxes;

   public final BoolVar[][][] ΔX;
   public final BoolVar[][][] ΔY;
   public final BoolVar[][][] ΔZ;
   public final BoolVar[][][] ΔK;

   public final IntVar[] nbActives;

   public AESGlobalRound(
         int r,
         int objStep1,
         KeyBits keyBits
   ) {
      this.em = new ExtendedModel("AES Global[1-5]");
      this.keyBits = keyBits;

      ΔX = em.boolVarTensor3("ΔX", r, 4, 4);
      ΔY = em.boolVarTensor3("ΔY/ΔSX", r, 4, 4);
      ΔZ = new BoolVar[r][4][4];
      ΔK = em.boolVarTensor3("ΔK", r, 4, 5);

      // ΔY = SR(SBoxPropagator(ΔX)) = SR(ΔX)
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               em.equals(ΔY[i][j][k], ΔX[i][j][(j + k) % 4]);
            }
         }
      }

      for (int i = 0; i < r - 1; i++) {
         ΔZ[i] = em.boolVarMatrix("ΔZ[" + i + "]", 4, 4);
      }
      ΔZ[r - 1] = ΔY[r - 1];

      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            if (keyBits.isSBRound(i)) {
               if (keyBits == AES_256 && i % 2 == 0) {
                  em.equals(ΔK[i][j][4], ΔK[i][j][getNbCol(i)]);
               } else {
                  em.equals(ΔK[i][j][4], ΔK[i][(j + 1) % 4][getNbCol(i)]);
               }
            }
         }
      }

      List<BoolVar> sBoxesList = new ArrayList<>();
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               sBoxesList.add(ΔX[i][j][k]);
            }
         }
      }
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            if (keyBits.isSBRound(i)) {
               sBoxesList.add(ΔK[i][j][4]);
            }
         }
      }

      // ARK
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               em.abstractXor(ΔK[i + 1][j][k], ΔX[i + 1][j][k], ΔZ[i][j][k]);
            }
         }
      }

      // MixColumn
      for (int i = 0; i < r - 1; i++) {
         for (int k = 0; k < 4; k++) {
            BoolVar[] deltas = new BoolVar[4 * 2];
            int cpt = 0;
            for (int j = 0; j < 4; j++) {
               deltas[cpt++] = ΔY[i][j][k];
               deltas[cpt++] = ΔZ[i][j][k];
            }
            em.sum(deltas, "=", em.intVar(intArrayOf(0, 5, 6, 7, 8)));
         }
      }

      // KeySchedule
      for (int i = 1; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               if (!keyBits.isInitialKey(i, k)) {
                  Pair<BytePosition, BytePosition> xorKS = keyBits.xorKeySchedulePi(i, j, k);
                  BytePosition pos1 = xorKS._0;
                  BytePosition pos2 = xorKS._1;
                  em.abstractXor(
                        ΔK[i][j][k],
                        ΔK[pos1.i][pos1.j][pos1.k],
                        ΔK[pos2.i][pos2.j][pos2.k]
                  );
               }
            }
         }
      }

      BoolVar[][][][][] DY2 = new BoolVar[4][r - 1][4][r - 1][4];
      BoolVar[][][][][] DZ2 = new BoolVar[4][r - 1][4][r - 1][4];

      // MDS Constraint
      for (int i1 = 0; i1 < r - 1; i1++) {
         for (int k1 = 0; k1 < 4; k1++) {
            for (int i2 = i1; i2 < r - 1; i2++) {
               int firstk2 = 0;
               if (i2 == i1) firstk2 = k1 + 1;
               for (int k2 = firstk2; k2 < 4; k2++) {
                  for (int j = 0; j < 4; j++) {
                     DY2[j][i1][k1][i2][k2] = em.boolVar("diffY[" + j + "][" + i1 + "][" + k1 + "][" + i2 + "][" + k2 + "]");
                     em.abstractXor(ΔY[i1][j][k1], ΔY[i2][j][k2], DY2[j][i1][k1][i2][k2]);
                     DZ2[j][i1][k1][i2][k2] = em.boolVar("diffZ[" + j + "][" + i1 + "][" + k1 + "][" + i2 + "][" + k2 + "]");
                     em.abstractXor(ΔZ[i1][j][k1], ΔZ[i2][j][k2], DZ2[j][i1][k1][i2][k2]);
                  }
                  em.sum(arrayOf(
                        DY2[0][i1][k1][i2][k2], DY2[1][i1][k1][i2][k2], DY2[2][i1][k1][i2][k2], DY2[3][i1][k1][i2][k2],
                        DZ2[0][i1][k1][i2][k2], DZ2[1][i1][k1][i2][k2], DZ2[2][i1][k1][i2][k2], DZ2[3][i1][k1][i2][k2]),
                        "=", em.intVar(intArrayOf(0, 5, 6, 7, 8))
                  );
               }
            }
         }
      }

      DeconstructedModel dm = em.build(
            new FullInferenceEngine(),
            new FullRulesApplier()
      );

      this.m = dm.model;
      this.constraintsOf = dm.constraintsOf;
      this.propagator = dm.propagator;

      nbActives = new IntVar[r];
      for (int i = 0; i < r; i++) {
         nbActives[i] = em.intVar("nbActives[" + i + "]", 0, objStep1);
         List<BoolVar> round = new ArrayList<>();
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               round.add(ΔX[i][j][k]);
            }
            if (keyBits.isSBRound(i)) {
               round.add(ΔK[i][j][4]);
            }
         }
         em.sum(asArray(round), "=", nbActives[i]);
      }

      em.sum(nbActives, "=", objStep1);
      sBoxes = asArray(sBoxesList);
   }

   private int getNbCol(int r) {
      if (keyBits == AES_128) {
         return 3;
      } else if (keyBits == AES_192) {
         if (r % 3 == 1) {
            return 1;
         } else {
            return 3;
         }
      } else if (keyBits == AES_256) {
         return 3;
      }
      throw new IllegalStateException();
   }

   private BoolVar[] asArray(List<BoolVar> vars) {
      BoolVar[] result = new BoolVar[vars.size()];
      int cpt = 0;
      for (BoolVar var : vars) {
         result[cpt++] = var;
      }
      return result;
   }

}

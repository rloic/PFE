package com.github.rloic.aes;

import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.common.collections.BytePosition;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.util.Pair;
import com.github.rloic.wip.WeightedConstraint;
import com.github.rloic.xorconstraint.AbstractXORPropagator;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.Variable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;

@SuppressWarnings("NonAsciiCharacters")
public class AESGlobalMC {

   private final ExtendedModel em;
   private final int r;
   private final KeyBits keyBits;

   public final Model m;
   public final AbstractXORPropagator propagator;
   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   public final BoolVar[] sBoxes;
   public final BoolVar[] varsToAssign;

   public final BoolVar[][][] ΔX;
   public final BoolVar[][][] ΔY;
   public final BoolVar[][][] ΔZ;
   public final BoolVar[][][] ΔK;

   public AESGlobalMC(
         int r,
         int objStep1,
         KeyBits keyBits
   ) {
      this.em = new ExtendedModel("AES Global[1-5]");
      this.r = r;
      this.keyBits = keyBits;

      ΔX = em.boolVar("ΔX", r, 4, 4);
      ΔY = em.boolVar("ΔY/ΔSX", r, 4, 4);
      ΔZ = new BoolVar[r][4][4];
      ΔK = em.boolVar("ΔK", r, 4, 5);

      // ΔY = SR(SBoxPropagator(ΔX)) \approx SR(ΔX)
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               em.equals(ΔY[i][j][k], ΔX[i][j][(j + k) % 4]);
            }
         }
      }

      for (int i = 0; i < r - 1; i++) {
         ΔZ[i] = em.boolVar("ΔZ[" + i + "]", 4, 4);
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
      sBoxes = new BoolVar[sBoxesList.size()];
      sBoxesList.toArray(sBoxes);
      em.sum(sBoxes, "=", objStep1);

      // ARK
      for (int i = 0; i < r - 1; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               em.abstractXor(ΔK[i + 1][j][k], ΔX[i + 1][j][k], ΔZ[i][j][k]);
            }
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

      BoolVar[][][] Δ2Y = em.boolVar("Δ2Y/diff_δY_δ3Y", r - 1, 4, 4);
      BoolVar[][][] Δ3Y = em.boolVar("Δ3Y/diff_δY_δ2Y", r - 1, 4, 4);

      for (int i = 0; i <= r - 2; i++) {
         for (int k = 0; k <= 3; k++) {
            for (int j = 0; j <= 3; j++) {
               em.abstractXor(ΔY[i][j][k], Δ2Y[i][j][k], Δ3Y[i][j][k]);
               em.equals(ΔY[i][j][k], Δ2Y[i][j][k], Δ3Y[i][j][k]);
            }

            em.abstractXor(ΔZ[i][0][k], Δ2Y[i][0][k], Δ3Y[i][1][k], ΔY[i][2][k], ΔY[i][3][k]);
            em.abstractXor(ΔZ[i][1][k], ΔY[i][0][k], Δ2Y[i][1][k], Δ3Y[i][2][k], ΔY[i][3][k]);
            em.abstractXor(ΔZ[i][2][k], ΔY[i][0][k], ΔY[i][1][k], Δ2Y[i][2][k], Δ3Y[i][3][k]);
            em.abstractXor(ΔZ[i][3][k], Δ3Y[i][0][k], ΔY[i][1][k], ΔY[i][2][k], Δ2Y[i][3][k]);
         }
      }

      BoolVar[][][][][][] diffδYδY = new BoolVar[r - 1][4][4][r - 1][4][4];
      BoolVar[][][][][][] diffδ2Yδ2Y = new BoolVar[r - 1][4][4][r - 1][4][4];
      BoolVar[][][][][][] diffδ3Yδ3Y = new BoolVar[r - 1][4][4][r - 1][4][4];

      for (int i1 = 0; i1 < r - 1; i1++) {
         for (int k1 = 0; k1 < 4; k1++) {
            for (int i2 = 0; i2 < r - 1; i2++) {
               for (int k2 = 0; k2 < 4; k2++) {
                  for (int j1 = 0; j1 < 4; j1++) {
                     for (int j2 = 0; j2 < 4; j2++) {
                        if (i1 != i2 || j1 != j2 || k1 != k2) {
                           diffδYδY[i1][j1][k1][i2][j2][k2] = makeDiffOf(ΔY[i1][j1][k1], ΔY[i2][j2][k2]);
                           diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2] = makeDiffOf(Δ2Y[i1][j1][k1], Δ2Y[i2][j2][k2]);
                           diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2] = makeDiffOf(Δ3Y[i1][j1][k1], Δ3Y[i2][j2][k2]);

                           em.equals(diffδYδY[i1][j1][k1][i2][j2][k2], diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2], diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2]);
                           em.abstractXor(diffδYδY[i1][j1][k1][i2][j2][k2], diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2], diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2]);
                        }
                     }
                  }
               }
            }
         }
      }

      varsToAssign = new BoolVar[3 * r * 4 * 4];
      int cpt = 0;
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               varsToAssign[cpt++] = ΔX[i][j][k];
               varsToAssign[cpt++] = ΔK[i][j][k];
               varsToAssign[cpt++] = ΔZ[i][j][k];
            }
         }
      }

      DeconstructedModel dm = em.build(
            new FullInferenceEngine(),
            new FullRulesApplier()
      );

      this.m = dm.model;

      BoolVar[][][][][][] diffδYδ2Y = new BoolVar[r - 1][4][4][r - 1][4][4];
      BoolVar[][][][][][] diffδYδ3Y = new BoolVar[r - 1][4][4][r - 1][4][4];
      BoolVar[][][][][][] diffδ2Yδ3Y = new BoolVar[r - 1][4][4][r - 1][4][4];

      for (int i1 = 0; i1 < r - 1; i1++) {
         for (int k1 = 0; k1 < 4; k1++) {
            for (int i2 = 0; i2 < r - 1; i2++) {
               for (int k2 = 0; k2 < 4; k2++) {
                  for (int j1 = 0; j1 < 4; j1++) {
                     for (int j2 = 0; j2 < 4; j2++) {
                        if (i1 != i2 || j1 != j2 || k1 != k2) {
                           diffδYδ2Y[i1][j1][k1][i2][j2][k2] = em.boolVar("diff_δY_δ2Y[" + i1 + "][" + j1 + "][" + k1 + "][" + i2 + "][" + j2 + "][" + k2 + "]");
                           diffδYδ3Y[i1][j1][k1][i2][j2][k2] = em.boolVar("diff_δY_δ3Y[" + i1 + "][" + j1 + "][" + k1 + "][" + i2 + "][" + j2 + "][" + k2 + "]");
                           diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2] = em.boolVar("diff_δ2Y_δ3Y[" + i1 + "][" + j1 + "][" + k1 + "][" + i2 + "][" + j2 + "][" + k2 + "]");
                        }
                     }
                  }
               }
            }
         }
      }

      final int EQUAL = 0;
      final int DIFFERENT = 1;

      for (int i1 = 0; i1 < r - 1; i1++) {
         for (int k1 = 0; k1 < 4; k1++) {
            for (int i2 = 0; i2 < r - 1; i2++) {
               for (int k2 = 0; k2 < 4; k2++) {
                  for (int j1 = 0; j1 < 4; j1++) {
                     for (int j2 = 0; j2 < 4; j2++) {
                        if (i1 != i2 || j1 != j2 || k1 != k2) {
                           // ( δY == δY' => δY != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δY' => δ2Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δY' => δY != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δY' => δ3Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δY' => δ2Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );
                           // ( δY == δY' => δ3Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δY' => δY == δY') true
                           // Always true

                           // ( δY == δY' => δ2Y == δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 equal(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                           );


                           // ( δY == δY' => δ3Y == δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδY[i1][j1][k1][i2][j2][k2])
                                 ),
                                 equal(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );


                           // ( δY == δ2Y' => δY != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδY[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ2Y' => δY != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                           );


                           // ( δY == δ2Y' => δ3Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δ2Y' => δ2Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i2][j2][k2][i1][j1][k1])
                           );
                           // checked

                           // ( δY == δ2Y' => δ2Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ2Y' => δ2Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );
                           // ( δY == δ2Y' => δ3Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δ2Y' => δ3Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ3Y' => δY != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδY[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ3Y' => δY != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ3Y' => δ2Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δ3Y' => δ2Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ3Y' => δ2Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ3Y' => δ3Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δ3Y' => δ3Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δY == δ3Y' => δ3Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δY == δ3Y' => δY == δ3Y') true
                           // Always true

                           // ( δ2Y == δY' => δY != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ2Y' => δY != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ2Y' => δY != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ2Y' => δ2Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ2Y == δ2Y' => δ2Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ2Y' => δ3Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ2Y == δ2Y' => δ3Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ2Y == δ2Y' => δY == δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδY[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ2Y' => δ2Y == δ2Y') true
                           // Aways true

                           // ( δ2Y == δ2Y' => δ3Y == δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδY[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ3Y' => δY != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδY[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ3Y' => δY != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ3Y' => δY != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ3Y' => δ2Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ2Y == δ3Y' => δ2Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ3Y' => δ3Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ2Y == δ3Y' => δ3Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ2Y == δ3Y' => δ3Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ2Y == δ3Y' => δ2Y == δ3Y') true
                           // Always true

                           // ( δ3Y == δ3Y' => δY != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ3Y == δ3Y' => δY != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ3Y == δ3Y' => δ2Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ2Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ3Y == δ3Y' => δ2Y != δ3Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ3Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ3Y == δ3Y' => δ3Y != δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ3Y == δ3Y' => δ3Y != δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδ3Y[i2][j2][k2][i1][j1][k1])
                           );

                           // ( δ3Y == δ3Y' => δY == δY') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδYδY[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ3Y == δ3Y' => δ2Y == δ2Y') true
                           m.ifThen(
                                 m.and(
                                       ΔY[i1][j1][k1],
                                       areEqual(diffδ3Yδ3Y[i1][j1][k1][i2][j2][k2])
                                 ),
                                 different(diffδ2Yδ2Y[i1][j1][k1][i2][j2][k2])
                           );

                           // ( δ3Y == δ3Y' => δ3Y == δ3Y') true
                           // Always true

                        }
                     }
                  }
               }
            }
         }
      }

      this.constraintsOf = dm.constraintsOf;
      this.propagator = dm.propagator;

   }

   private Constraint equal(BoolVar diff) {
      return m.arithm(diff, "=", 0);
   }

   private BoolVar areEqual(BoolVar diff) {
      return equal(diff).reify();
   }

   private Constraint different(BoolVar diff) {
      return m.arithm(diff, "=", 1);
   }

   private BoolVar makeDiffOf(BoolVar... vars) {
      BoolVar res = em.boolVar("TMP = " + Arrays.stream(vars).map(Variable::getName).collect(Collectors.joining()));
      BoolVar[] equation = new BoolVar[vars.length + 1];
      System.arraycopy(vars, 0, equation, 0, vars.length);
      equation[equation.length - 1] = res;
      em.abstractXor(equation);
      return res;
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

}

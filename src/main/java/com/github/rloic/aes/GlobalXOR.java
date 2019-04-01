package com.github.rloic.aes;

import com.github.rloic.common.abstraction.MathSet;
import com.github.rloic.common.abstraction.XOREquation;
import com.github.rloic.common.collections.BytePosition;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.util.Logger;
import com.github.rloic.util.Pair;
import com.github.rloic.xorconstraint.BasePropagator;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.*;
import java.util.stream.Collectors;

import static com.github.rloic.aes.KeyBits.AES128.AES_128;
import static com.github.rloic.aes.KeyBits.AES192.AES_192;
import static com.github.rloic.aes.KeyBits.AES256.AES_256;
import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;
import static com.github.rloic.common.collections.ArrayExtensions.intArrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class GlobalXOR {

   public final Model m;
   public final BoolVar[] sBoxes;
   private final int r;
   private final KeyBits KEY_BITS;
   public final BoolVar[] assignedVar;

   private final List<BoolVar> variables = new ArrayList<>();
   private final List<BoolVar[]> equations = new ArrayList<>();

   public final BasePropagator propagator;

   public GlobalXOR(
         int r,
         int objStep1,
         KeyBits keyBits
   ) {
      this.m = new Model("Advanced Model(r=" + r + ", objStep=" + objStep1 + ")");
      this.r = r;
      this.KEY_BITS = keyBits;

      BoolVar[][][] ΔX = buildΔX(r, 4, 4);
      BoolVar[][][] ΔY = new BoolVar[r][4][4];
      // C'4 = SR: ∀i ∈ [0, r − 1], ∀j, k ∈ [0, 3], ∆Y[i][j][k] = ∆X[i][j][(j + k) %4]
      for (int i = 0; i <= r - 1; i++) {
         for (int j = 0; j <= 3; j++) {
            for (int k = 0; k <= 3; k++) {
               ΔY[i][j][k] = ΔX[i][j][(j + k) % 4];
            }
         }
      }
      BoolVar[][][] ΔZ = c6(ΔY);
      BoolVar[][][] ΔK = buildΔK(r, 4, 5);
      sBoxes = c1(ΔX, ΔK, objStep1);

      // C'3 = ARK: ∀i ∈ [0, r − 2], ∀j, k ∈ [0, 3], XOR(∆Z[i][j][k], ∆K[i+1][j][k], ∆X[i+1][j][k])
      for (int i = 0; i <= r - 2; i++) {
         for (int j = 0; j <= 3; j++) {
            for (int k = 0; k <= 3; k++) {
               appendToGlobalXor(ΔK[i + 1][j][k], ΔX[i + 1][j][k], ΔZ[i][j][k]);
               m.sum(new IntVar[]{ΔK[i + 1][j][k], ΔX[i + 1][j][k], ΔZ[i][j][k]}, "!=", 1).post();
            }
         }
      }
      // C'5 = MC: ∀i ∈ [0, r − 2], ∀k ∈ [0, 3], Sum(j ∈ 0..3) { ∆Y[i][j][k] + ∆Z[i][j][k] } ∈ {0, 5, 6, 7, 8}
      for (int i = 0; i <= r - 2; i++) {
         for (int k = 0; k <= 3; k++) {
            m.sum(arrayOf(ΔY[i][0][k], ΔY[i][1][k], ΔY[i][2][k], ΔY[i][3][k],
                  ΔZ[i][0][k], ΔZ[i][1][k], ΔZ[i][2][k], ΔZ[i][3][k]
            ), "=", m.intVar(intArrayOf(0, 5, 6, 7, 8))).post();
         }
      }

      MathSet<XOREquation> xorEq = xorEq();

      // KeySchedule
      for (XOREquation eq : xorEq) {
         List<BytePosition> elements = new ArrayList<>(eq);
         appendToGlobalXor(
               ΔK[elements.get(0).i][elements.get(0).j][elements.get(0).k],
               ΔK[elements.get(1).i][elements.get(1).j][elements.get(1).k],
               ΔK[elements.get(2).i][elements.get(2).j][elements.get(2).k]
         );
      }

      BoolVar[][][][][] DY2 = new BoolVar[4][r - 1][4][r - 1][4];
      BoolVar[][][][][] DZ2 = new BoolVar[4][r - 1][4][r - 1][4];

      // MDS constraint
      for (int i1 = 0; i1 < r - 1; i1++) {
         for (int k1 = 0; k1 < 4; k1++) {
            for (int i2 = i1; i2 < r - 1; i2++) {
               int firstk2 = 0;
               if (i2 == i1) firstk2 = k1 + 1;
               for (int k2 = firstk2; k2 < 4; k2++) {
                  for (int j = 0; j < 4; j++) {
                     DY2[j][i1][k1][i2][k2] = m.boolVar("diffY[" + j + "][" + i1 + "][" + k1 + "][" + i2 + "][" + k2 + "]");
                     appendToGlobalXor(ΔY[i1][j][k1], ΔY[i2][j][k2], DY2[j][i1][k1][i2][k2]);
                     m.sum(new IntVar[]{DY2[j][i1][k1][i2][k2], ΔY[i1][j][k1], ΔY[i2][j][k2]}, "!=", 1).post();
                     DZ2[j][i1][k1][i2][k2] = m.boolVar("diffZ[" + j + "][" + i1 + "][" + k1 + "][" + i2 + "][" + k2 + "]");
                     appendToGlobalXor(ΔZ[i1][j][k1], ΔZ[i2][j][k2], DZ2[j][i1][k1][i2][k2]);
                     m.sum(new IntVar[]{DZ2[j][i1][k1][i2][k2], ΔZ[i1][j][k1], ΔZ[i2][j][k2]}, "!=", 1).post();
                  }
                  m.sum(arrayOf(
                        DY2[0][i1][k1][i2][k2], DY2[1][i1][k1][i2][k2], DY2[2][i1][k1][i2][k2], DY2[3][i1][k1][i2][k2],
                        DZ2[0][i1][k1][i2][k2], DZ2[1][i1][k1][i2][k2], DZ2[2][i1][k1][i2][k2], DZ2[3][i1][k1][i2][k2]),
                        "=", m.intVar(intArrayOf(0, 5, 6, 7, 8))).post();
               }
            }
         }
      }
/*
      MathSet<XOREquation> extendedXorEq = new MathSet<>(xorEq);
      extendedXorEq.addAll(combineXor(xorEq, xorEq));
      BoolVar[][][][][] DK2 = new BoolVar[4][r][4][r][4];
      // j ∈ [0, 3] i ∈ [0, r - 1], k ∈ [0, 3 + 1] pour δSK
      for (int j = 0; j <= 3; j++) {
         for (int i1 = 0; i1 <= r - 2; i1++) {
            for (int k1 = 0; k1 <= 3; k1++) {
               for (int i2 = i1; i2 <= r - 2; i2++) {
                  int k2Init = (i1 == i2) ? k1 + 1 : 0;
                  for (int k2 = k2Init; k2 <= 3; k2++) {
                     // C'7: diff(δB1,δB2) = diff(δB2,δB1)
                     if (contains(sBoxes, ΔK[i1][j][k1]) && contains(sBoxes, ΔK[i2][j][k2])) {
                        BoolVar diff_δk1_δk2 = m.boolVar("diffK[" + j + "][" + i1 + "][" + k1 + "][" + i2 + "][" + k2 + "]");
                        DK2[j][i1][k1][i2][k2] = diff_δk1_δk2;
                        DK2[j][i2][k2][i1][k1] = diff_δk1_δk2;
                        appendToGlobalXor(diff_δk1_δk2, ΔK[i1][j][k1], ΔK[i2][j][k2]);
                        m.sum(new IntVar[]{diff_δk1_δk2, ΔK[i1][j][k1], ΔK[i2][j][k2]}, "!=", 1).post();
                     }
                  }
               }
            }
         }
      }

      c10c11(DK2, ΔK, extendedXorEq);
*/
      assignedVar = new BoolVar[3 * r * 4 * 4];
      int cpt = 0;
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < 4; j++) {
            for (int k = 0; k < 4; k++) {
               assignedVar[cpt++] = ΔX[i][j][k];
               assignedVar[cpt++] = ΔK[i][j][k];
               assignedVar[cpt++] = ΔZ[i][j][k];
            }
         }
      }

      BoolVar[] vars = new BoolVar[variables.size()];
      variables.toArray(vars);
      BoolVar[][] eqs = new BoolVar[equations.size()][];
      equations.toArray(eqs);

      this.propagator = new BasePropagator(
            vars,
            eqs,
            new FullInferenceEngine(),
            new FullRulesApplier(),
            m.getSolver()
      );
      m.post(new Constraint("GlobalXor", propagator));
   }

   private BoolVar[][][] buildΔX(int r, int rows, int columns) {
      BoolVar[][][] result = new BoolVar[r][][];
      for (int i = 0; i < r; i++) result[i] = m.boolVarMatrix("ΔX[" + i + "]", rows, columns);
      return result;
   }

   private BoolVar[][][] buildΔK(int r, int rows, int columns) {
      BoolVar[][][] result = new BoolVar[r][rows][columns];
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < rows; j++) {
            for (int k = 0; k < columns - 1; k++) {
               result[i][j][k] = m.boolVar("ΔK[" + i + ", " + j + ", " + k + "]");
            }
         }
      }
      for (int i = 0; i < r; i++) {
         for (int j = 0; j < rows; j++) {
            if (KEY_BITS.isSBRound(i)) {
               if (KEY_BITS == AES_256 && i % 2 == 0) {
                  result[i][j][4] = result[i][j][getNbCol(i)];
               } else {
                  result[i][j][4] = result[i][(j + 1) % 4][getNbCol(i)];
               }
            }
         }
      }
      return result;
   }

   private int getNbCol(int r) {
      if (KEY_BITS == AES_128) {
         return 3;
      } else if (KEY_BITS == AES_192) {
         if (r % 3 == 1) {
            return 1;
         } else {
            return 3;
         }
      } else if (KEY_BITS == AES_256) {
         return 3;
      }
      throw new IllegalStateException();
   }

   private BoolVar[] c1(
         BoolVar[][][] ΔX,
         BoolVar[][][] ΔK,
         int objStep1
   ) {
      List<BoolVar> sBoxesList = new ArrayList<>();
      // C1: objStep1 = Sum(δB ∈ Sboxes_{l}) { ∆B }
      for (int i = 0; i <= r - 1; i++) {
         for (int j = 0; j <= 3; j++) {
            for (int k = 0; k <= 3; k++) {
               sBoxesList.add(ΔX[i][j][k]);
            }
         }
      }
      for (int i = 0; i <= r - 1; i++) {
         for (int j = 0; j <= 3; j++) {
            if (KEY_BITS.isSBRound(i)) {
               sBoxesList.add(ΔK[i][j][4]);
            }
         }
      }
      Collections.reverse(sBoxesList);
      BoolVar[] sBoxes = new BoolVar[sBoxesList.size()];
      sBoxesList.toArray(sBoxes);
      m.sum(sBoxes, "=", objStep1).post();
      return sBoxes;
   }

   private BoolVar[][][] c6(BoolVar[][][] ΔY) {
      BoolVar[][][] ΔZ = new BoolVar[r][4][4];
      // ∀j, k ∈ [0, 3]
      for (int j = 0; j <= 3; j++) {
         for (int k = 0; k <= 3; k++) {
            for (int i = 0; i <= r - 2; i++) {
               ΔZ[i][j][k] = m.boolVar("ΔZ[" + i + "][" + j + "][" + k + "]");
            }
            // C6: ∆Z[r−1][j][k] = ∆Y[r−1][j][k]
            ΔZ[r - 1][j][k] = ΔY[r - 1][j][k];
         }
      }
      return ΔZ;
   }

   private void appendToGlobalXor(BoolVar A, BoolVar B, BoolVar C) {
      if (!variables.contains(A)) {
         variables.add(A);
      }
      if (!variables.contains(B)) {
         variables.add(B);
      }
      if (!variables.contains(C)) {
         variables.add(C);
      }
      equations.add(arrayOf(A, B, C));
   }

   private MathSet<XOREquation> xorEq() {
      MathSet<XOREquation> initialKeyScheduleXORs = new MathSet<>();
      for (int i = 1; i <= r - 1; i++) {
         for (int j = 0; j <= 3; j++) {
            for (int k = 0; k <= 3; k++) {
               if (!KEY_BITS.isInitialKey(i, k)) {
                  Pair<BytePosition, BytePosition> xorKeySchedule = KEY_BITS.xorKeySchedulePi(i, j, k);
                  XOREquation res = new XOREquation(new BytePosition(i, j, k), xorKeySchedule._0, xorKeySchedule._1);
                  initialKeyScheduleXORs.add(res);
               }
            }
         }
      }

      return initialKeyScheduleXORs;
   }

   private MathSet<XOREquation> combineXor(MathSet<XOREquation> lhs, MathSet<XOREquation> rhs) {
      if (lhs.isEmpty()) return new MathSet<>();
      MathSet<XOREquation> newEquationsSet = new MathSet<>();
      for (XOREquation equation1 : lhs) {
         for (XOREquation equation2 : rhs) {
            if (!equation1.equals(equation2)) {
               XOREquation mergedEquation = equation1.merge(equation2);
               if (mergedEquation.size() < Math.min(equation1.size() + equation2.size(), 5) && !rhs.contains(mergedEquation)) {
                  newEquationsSet.add(mergedEquation);
               }
            }
         }
      }
      Logger.debug("    [CombinedXOR] Number of new XOR = " + newEquationsSet.size());
      return newEquationsSet.union(combineXor(newEquationsSet, newEquationsSet.union(rhs)));
   }

   // C'10 CHECKED
   private void c10(
         BoolVar[][][][][] diffK,
         BoolVar[][][] ΔK,
         BytePosition B1,
         BytePosition B2,
         BytePosition B3
   ) {
      if (B1.j == B2.j && B1.j == B3.j && B1.k < 4 && B2.k < 4 && B3.k < 4) {
         // C11: (diff(δB1,δB2) = ∆B_{3}) ∧ (diff(δB1,δB3) = ∆B_{2}) ∧ (diff(δB2,δB3) = ∆B_{1})
         if (diffOf(diffK, B1, B2) != null) {
            m.arithm(diffOf(diffK, B1, B2), "=", deltaOf(ΔK, B3)).post();
         }
         if (diffOf(diffK, B1, B3) != null) {
            m.arithm(diffOf(diffK, B1, B3), "=", deltaOf(ΔK, B2)).post();
         }
         if (diffOf(diffK, B2, B3) != null) {
            m.arithm(diffOf(diffK, B2, B3), "=", deltaOf(ΔK, B1)).post();
         }
      }
   }


   // C'11 CHECKED
   private void c11(
         BoolVar[][][][][] diffK,
         BytePosition B1,
         BytePosition B2,
         BytePosition B3,
         BytePosition B4
   ) {
      if (B1.j == B2.j && B1.j == B3.j && B1.j == B4.j && B1.k < 4 && B2.k < 4 && B3.k < 4 && B4.k < 4) {

         // C11: (diff(δB1,δB2) = diff(δB3,δB4)) ∧ (diff(δB1,δB3) = diff(δB2,δB4)) ∧ (diff(δB1,δB4) = diff(δB2,δB3))
         if (diffOf(diffK, B1, B2) != null && diffOf(diffK, B3, B4) != null) {
            m.arithm(diffOf(diffK, B1, B2), "=", diffOf(diffK, B3, B4)).post();
         }
         if (diffOf(diffK, B1, B3) != null && diffOf(diffK, B2, B4) != null) {
            m.arithm(diffOf(diffK, B1, B3), "=", diffOf(diffK, B2, B4)).post();
         }
         if (diffOf(diffK, B1, B4) != null && diffOf(diffK, B2, B3) != null) {
            m.arithm(diffOf(diffK, B1, B4), "=", diffOf(diffK, B2, B3)).post();
         }
      }
   }

   // CHECKED
   private void c10c11(
         BoolVar[][][][][] diffK,
         BoolVar[][][] ΔK,
         MathSet<XOREquation> xorEq
   ) {
      for (XOREquation eq : xorEq) {
         List<BytePosition> elements = new ArrayList<>(eq);
         if (elements.size() == 3) {
            // C'10
            c10(diffK, ΔK, elements.get(0), elements.get(1), elements.get(2));
         } else {
            // C'11
            c11(diffK, elements.get(0), elements.get(1), elements.get(2), elements.get(3));
         }
      }
   }

   private BoolVar diffOf(BoolVar[][][][][] diff, BytePosition B1, BytePosition B2) {
      if (B1.j != B2.j)
         throw new IllegalArgumentException("B1.j must be equals to B2.j. Given: B1=" + B1 + ", B2=" + B2 + ".");
      return diff[B1.j][B1.i][B1.k][B2.i][B2.k];
   }

   private BoolVar deltaOf(BoolVar[][][] Δ, BytePosition B) {
      return Δ[B.i][B.j][B.k];
   }

   private <T> boolean contains(T[] array, T element) {
      int i = 0;
      while (i < array.length && array[i] != element) {
         i += 1;
      }
      return i != array.length;
   }

}

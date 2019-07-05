package com.github.rloic.aes;

import com.github.rloic.aes.gf.GaloisFieldMultiplication;
import com.github.rloic.aes.sbox.SBox;
import com.github.rloic.common.DeconstructedModel;
import com.github.rloic.common.ExtendedModel;
import com.github.rloic.paper.dancinglinks.inferenceengine.impl.FullInferenceEngine;
import com.github.rloic.paper.dancinglinks.rulesapplier.impl.FullRulesApplier;
import com.github.rloic.wip.WeightedConstraint;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;

import java.util.ArrayList;
import java.util.List;

import static com.github.rloic.common.collections.ArrayExtensions.arrayOf;

@SuppressWarnings("NonAsciiCharacters")
public class AESFullSteps {

   private final static String STRATEGY = "FC";
   private final static int[] PROBABILITIES = new int[]{0, 6, 7};

   private final List<IntVar> _probabilities = new ArrayList<>();
   private final List<BoolVar> _sBoxes = new ArrayList<>();
   private final ExtendedModel em = new ExtendedModel("AESFullSteps");

   // Public for solver
   public final IntVar objective;
   public final BoolVar[] sBoxes;
   public final BoolVar[] ΔX;
   public final BoolVar[] ΔSX;
   public final BoolVar[] ΔY;
   public final BoolVar[] ΔZ;

   public final Model m;
   public final Int2ObjectMap<List<WeightedConstraint>> constraintsOf;

   public AESFullSteps(KeyBits version, int Nr, int sb) {
      // Parameters
      int Nk = version.keyColumns;

      // Encryption Process
      ExtendedModel.Byte[][] δPlainText = em.byteVarMatrix("PlainText", 4, 4);
      ExtendedModel.Byte[][][] δX = new ExtendedModel.Byte[Nr][][];
      ExtendedModel.Byte[][][] δSX = new ExtendedModel.Byte[Nr][][];
      ExtendedModel.Byte[][][] δY = new ExtendedModel.Byte[Nr][][];
      ExtendedModel.Byte[][][] δZ = new ExtendedModel.Byte[Nr - 1][][];

      // KeySchedule
      ExtendedModel.Byte[][] δCipherKey = em.byteVarMatrix("CipherKey", 4, 4);
      ExtendedModel.Byte[][] δWK = keyExpansion(δCipherKey, Nk, Nr);

      // Initial round
      δX[0] = addRoundKey("X_0", δPlainText, subKey(δWK, 0));

      // Main rounds
      for (int i = 0; i < Nr - 1; i++) {
         δSX[i] = subBytes(δX[i]);
         δY[i] =  shiftRows(δSX[i]);
         δZ[i] =  mixColumn(δY[i]);
         δX[i + 1] = addRoundKey("X_i", δZ[i], subKey(δWK, i + 1));
      }

      // Last round
      δSX[Nr - 1] = subBytes(δX[Nr - 1]);
      δY[Nr - 1] = shiftRows(δSX[Nr - 1]);
      @SuppressWarnings("unused")
      ExtendedModel.Byte[][] δCipherText = addRoundKey("CipherText", δY[Nr - 1], subKey(δWK, Nr));

      IntVar[] probabilities = new IntVar[_probabilities.size()];
      _probabilities.toArray(probabilities);

      ΔX = abstraction(δX);
      ΔSX = abstraction(δSX);
      ΔY = abstraction(δY);
      ΔZ = abstraction(δZ);

      sBoxes = new BoolVar[_sBoxes.size()];
      _sBoxes.toArray(sBoxes);
      em.sum(sBoxes, "=", sb);

      objective = em.intVar("objective", new int[]{6 * sb, 7 * sb});
      em.sum(probabilities, "=", objective);

      DeconstructedModel dm = em.build(new FullInferenceEngine(), new FullRulesApplier());
      m = dm.model;
      constraintsOf = dm.constraintsOf;
   }

   // KeySchedule
   private ExtendedModel.Byte[][] keyExpansion(ExtendedModel.Byte[][] δCipherKey, int Nk, int Nr) {
      ExtendedModel.Byte[][] δWK = new ExtendedModel.Byte[4][(Nr + 1) * 4];

      for(int k = 0; k < Nk; k++) {
         for (int j = 0; j < 4; j++) {
            δWK[j][k] = δCipherKey[j][k];
         }
      }
      for (int k = Nk; k < (Nr + 1) * 4; k++) {
         if (k % Nk == 0) {
            // WK[*][k] = WK[*][k-Nk] xor SubWord(RotWord(WK[*][k-1]))
            setColumn(δWK, k,
                  columnXor(getColumn(δWK, k - Nk), subWord(rotWord(getColumn(δWK, k - 1))))
            );
         } else if (Nk > 6 && k % Nk == 4) {
            // WK[*][k] = WK[*][k-Nk] xor SubWord(WK[*][k-1])
            setColumn(δWK, k,
                  columnXor(getColumn(δWK, k - Nk), subWord(getColumn(δWK, k - 1)))
            );
         } else {
            // WK[*][k] = WK[*][k-Nk] xor WK[*][k-1]
            setColumn(δWK, k,
                  columnXor(getColumn(δWK, k - Nk), getColumn(δWK, k - 1))
            );
         }
      }
      return δWK;
   }

   private ExtendedModel.Byte[][] subKey(ExtendedModel.Byte[][] δWK, int i) {
      ExtendedModel.Byte[][] subKey = new ExtendedModel.Byte[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            subKey[j][k] = δWK[j][i * 4 + k];
         }
      }
      return subKey;
   }

   // KeySchedule sub-process
   private ExtendedModel.Byte[] rotWord(ExtendedModel.Byte[] column) {
      return new ExtendedModel.Byte[]{
            column[1],
            column[2],
            column[3],
            column[0]
      };
   }

   private ExtendedModel.Byte[] subWord(ExtendedModel.Byte[] K_i) {
      return new ExtendedModel.Byte[] {
            subBytes(K_i[0]),
            subBytes(K_i[1]),
            subBytes(K_i[2]),
            subBytes(K_i[3])
      };
   }

   // Main process
   private ExtendedModel.Byte[][] addRoundKey(
         String name,
         ExtendedModel.Byte[][] block,
         ExtendedModel.Byte[][] δSubKey_i
   ) {
      ExtendedModel.Byte[][] ark = new ExtendedModel.Byte[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            ark[j][k] = em.byteVar(name + "[" + j + "][" + k + "]");
            em.xor(ark[j][k], block[j][k], δSubKey_i[j][k]);
         }
      }
      return ark;
   }

   private ExtendedModel.Byte[][] subBytes(ExtendedModel.Byte[][] δX_i) {
      ExtendedModel.Byte[][] δSX_i = new ExtendedModel.Byte[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            δSX_i[j][k] = subBytes(δX_i[j][k]);
         }
      }
      return δSX_i;
   }

   private ExtendedModel.Byte[][] shiftRows(ExtendedModel.Byte[][] δSX_i) {
      ExtendedModel.Byte[][] δY_i = new ExtendedModel.Byte[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            δY_i[j][k] = δSX_i[j][(j + k) % 4];
         }
      }
      return δY_i;
   }

   private ExtendedModel.Byte[][] mixColumn(ExtendedModel.Byte[][] δ1Y_i) {
      ExtendedModel.Byte[][] δZ_i = new ExtendedModel.Byte[4][4];

      ExtendedModel.Byte[][] δ2Y_i = new ExtendedModel.Byte[4][4];
      ExtendedModel.Byte[][] δ3Y_i = new ExtendedModel.Byte[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            δ2Y_i[j][k] = times2(δ1Y_i[j][k]);
            δ3Y_i[j][k] = em.xorVar(δ1Y_i[j][k], δ2Y_i[j][k]);
         }
      }

      ExtendedModel.Byte[][] δ09Z_i = new ExtendedModel.Byte[4][4];
      ExtendedModel.Byte[][] δ11Z_i = new ExtendedModel.Byte[4][4];
      ExtendedModel.Byte[][] δ13Z_i = new ExtendedModel.Byte[4][4];
      ExtendedModel.Byte[][] δ14Z_i = new ExtendedModel.Byte[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            δ09Z_i[j][k] = times9(δ1Y_i[j][k]);
            δ11Z_i[j][k] = times11(δ1Y_i[j][k]);
            δ13Z_i[j][k] = times13(δ1Y_i[j][k]);
            δ14Z_i[j][k] = times14(δ1Y_i[j][k]);
         }
      }

      for (int k = 0; k < 4; k++) {
         δZ_i[0][k] = em.xorVar(δ2Y_i[0][k], δ3Y_i[1][k], δ1Y_i[2][k], δ1Y_i[3][k]);  // 2 3 1 1
         δZ_i[1][k] = em.xorVar(δ1Y_i[0][k], δ2Y_i[1][k], δ3Y_i[2][k], δ1Y_i[3][k]);  // 1 2 3 1
         δZ_i[2][k] = em.xorVar(δ1Y_i[0][k], δ1Y_i[1][k], δ2Y_i[2][k], δ3Y_i[3][k]);  // 1 1 2 3
         δZ_i[3][k] = em.xorVar(δ3Y_i[0][k], δ1Y_i[1][k], δ1Y_i[2][k], δ2Y_i[3][k]);  // 3 1 1 2

         ExtendedModel.Byte[] δinvY_i = new ExtendedModel.Byte[]{
               em.xorVar(δ14Z_i[0][k], δ11Z_i[1][k], δ13Z_i[2][k], δ09Z_i[3][k]),    // 14 11 13  9
               em.xorVar(δ09Z_i[0][k], δ14Z_i[1][k], δ11Z_i[2][k], δ13Z_i[3][k]),    //  9 14 11 13
               em.xorVar(δ13Z_i[0][k], δ09Z_i[1][k], δ14Z_i[2][k], δ11Z_i[3][k]),    // 13  9 14 11
               em.xorVar(δ11Z_i[0][k], δ13Z_i[1][k], δ09Z_i[2][k], δ14Z_i[3][k])     // 11 13  9 14
         };
         em.equals(δ1Y_i[0][k], δinvY_i[0]);
         em.equals(δ1Y_i[1][k], δinvY_i[1]);
         em.equals(δ1Y_i[2][k], δinvY_i[2]);
         em.equals(δ1Y_i[3][k], δinvY_i[3]);
      }

      return δZ_i;
   }

   // KeySchedule utils
   private ExtendedModel.Byte[] columnXor(ExtendedModel.Byte[] vecA, ExtendedModel.Byte[] vecB) {
      ExtendedModel.Byte[] result = new ExtendedModel.Byte[vecA.length];
      for (int i = 0; i < result.length; i++) {
         result[i] = em.xorVar(vecA[i], vecB[i]);
      }
      return result;
   }

   private ExtendedModel.Byte[] getColumn(ExtendedModel.Byte[][] M, int k) {
      ExtendedModel.Byte[] column = new ExtendedModel.Byte[M.length];
      for (int j = 0; j < M.length; j++) {
         column[j] = M[j][k];
      }
      return column;
   }

   private void setColumn(ExtendedModel.Byte[][] M, int k, ExtendedModel.Byte[] column) {
      for (int j = 0; j < M.length; j++) {
         M[j][k] = column[j];
      }
   }

   // Main utils
   private ExtendedModel.Byte subBytes(ExtendedModel.Byte δ) {
      ExtendedModel.Byte Sδ = em.byteVar("SB(" + δ.name + ")");
      IntVar p = newProbability();
      em.table(
            arrayOf(
                  δ.realization,
                  Sδ.realization,
                  p
            ),
            SBox.aes,
            STRATEGY
      );
      em.equals(δ.abstraction, Sδ.abstraction);
      if (!_sBoxes.contains(δ.abstraction)) {
         _sBoxes.add(δ.abstraction);
      }
      return Sδ;
   }

   private ExtendedModel.Byte times2(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx2 = em.byteVar("2 * " + δ.name);
      em.table(arrayOf(δ.realization, δx2.realization), GaloisFieldMultiplication.times2, STRATEGY);
      //em.equals(δ.abstraction, δx2.abstraction);
      return δx2;
   }

   private ExtendedModel.Byte times9(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx9 = em.byteVar("9 * " + δ.name);
      em.table(arrayOf(δ.realization, δx9.realization), GaloisFieldMultiplication.times9, STRATEGY);
      //em.equals(δ.abstraction, δx9.abstraction);
      return δx9;
   }

   private ExtendedModel.Byte times11(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx11 = em.byteVar("11 * " + δ.name);
      em.table(arrayOf(δ.realization, δx11.realization), GaloisFieldMultiplication.times11, STRATEGY);
      //em.equals(δ.abstraction, δx11.abstraction);
      return δx11;
   }

   private ExtendedModel.Byte times13(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx13 = em.byteVar("13 * " + δ.name);
      em.table(arrayOf(δ.realization, δx13.realization), GaloisFieldMultiplication.times13, STRATEGY);
      //em.equals(δ.abstraction, δx13.abstraction);
      return δx13;
   }

   private ExtendedModel.Byte times14(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx14 = em.byteVar("14 * " + δ.name);
      em.table(arrayOf(δ.realization, δx14.realization), GaloisFieldMultiplication.times14, STRATEGY);
      //em.equals(δ.abstraction, δx14.abstraction);
      return δx14;
   }

   private IntVar newProbability() {
      IntVar p = em.intVar("probability[" + _probabilities.size() + "]", PROBABILITIES);
      _probabilities.add(p);
      return p;
   }

   private BoolVar[] abstraction(ExtendedModel.Byte[][][] bytes) {
      int length = 0;
      for (int i = 0; i < bytes.length; i++) {
         for (int j = 0; j < bytes[i].length; j++) {
            length += bytes[i][j].length;
         }
      }
      BoolVar[] abstractions = new BoolVar[length];
      int cpt = 0;
      for (ExtendedModel.Byte[][] block : bytes) {
         for (ExtendedModel.Byte[] row : block) {
            for (ExtendedModel.Byte δ : row) {
               abstractions[cpt++] = δ.abstraction;
            }
         }
      }
      return abstractions;
   }

}

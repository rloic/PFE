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
      δX[0] = addRoundKey(δPlainText, subKey(δWK, 0));

      // Main rounds
      for (int i = 0; i < Nr - 1; i++) {
         δSX[i] = subBytes(δX[i]);
         δY[i] = shiftRows(δSX[i]);
         δZ[i] = mixColumn(δY[i]);
         δX[i + 1] = addRoundKey(δZ[i], subKey(δWK, i + 1));
      }

      // Last round
      δSX[Nr - 1] = subBytes(δX[Nr - 1]);
      δY[Nr - 1] = shiftRows(δSX[Nr - 1]);
      @SuppressWarnings("unused")
      ExtendedModel.Byte[][] δCipherText = addRoundKey(δY[Nr - 1], subKey(δWK, Nr));

      ΔX = abstraction(δX);
      ΔSX = abstraction(δSX);
      ΔY = abstraction(δY);
      ΔZ = abstraction(δZ);

      sBoxes = new BoolVar[_sBoxes.size()];
      _sBoxes.toArray(sBoxes);
      em.sum(sBoxes, "=", sb);

      IntVar[] probabilities = new IntVar[_probabilities.size()];
      _probabilities.toArray(probabilities);
      objective = em.intVar("objective", 6 * sb, 7 * sb);
      em.sum(probabilities, "=", objective);

      DeconstructedModel dm = em.build(new FullInferenceEngine(), new FullRulesApplier());
      m = dm.model;
      constraintsOf = dm.constraintsOf;
   }

   // KeySchedule
   private ExtendedModel.Byte[][] keyExpansion(ExtendedModel.Byte[][] δCipherKey, int Nk, int Nr) {
      ExtendedModel.Byte[][] δWK = new ExtendedModel.Byte[4][(Nr + 1) * 4];
      for (int k = 0; k < Nk; k++) {
         // WK[*][k] = δCipherKey[*][k]
         setColumn(δWK, k, getColumn(δCipherKey, k));
      }
      for (int k = Nk; k < (Nr + 1) * 4; k++) {
         if (k % Nk == 0) {
            // WK[*][k] = WK[*][k-Nk] xor SubWord(RotWord(WK[*][k-1]))
            setColumn(δWK, k,
                  elementsWiseXor(getColumn(δWK, k - Nk), subWord(rotWord(getColumn(δWK, k - 1))))
            );
         } else if (Nk > 6 && k % Nk == 4) {
            // WK[*][k] = WK[*][k-Nk] xor SubWord(WK[*][k-1])
            setColumn(δWK, k,
                  elementsWiseXor(getColumn(δWK, k - Nk), subWord(getColumn(δWK, k - 1)))
            );
         } else {
            // WK[*][k] = WK[*][k-Nk] xor WK[*][k-1]
            setColumn(δWK, k,
                  elementsWiseXor(getColumn(δWK, k - Nk), getColumn(δWK, k - 1))
            );
         }
      }
      return δWK;
   }

   private ExtendedModel.Byte[][] subKey(ExtendedModel.Byte[][] δWK, int i) {
      return block((j, k) -> δWK[j][i * 4 + k]);
   }

   // KeySchedule sub-process
   private ExtendedModel.Byte[] rotWord(ExtendedModel.Byte[] column) {
      return column((j) -> column[(j + 1) % 4]);
   }

   private ExtendedModel.Byte[] subWord(ExtendedModel.Byte[] K_i) {
      return column((j) -> subBytes("SK_i", K_i[j]));
   }

   // Encryption process
   private ExtendedModel.Byte[][] addRoundKey(
         ExtendedModel.Byte[][] block,
         ExtendedModel.Byte[][] subKey_i
   ) {
      return block((j, k) -> em.xorVar(block[j][k], subKey_i[j][k]));
   }

   private ExtendedModel.Byte[][] subBytes(ExtendedModel.Byte[][] δX_i) {
      return block((j, k) -> subBytes("SX_i", δX_i[j][k]));
   }

   private ExtendedModel.Byte[][] shiftRows(ExtendedModel.Byte[][] δSX_i) {
      return block((j, k) -> δSX_i[j][(j + k) % 4]);
   }

   private ExtendedModel.Byte[][] mixColumn(ExtendedModel.Byte[][] _1Y_i) {
      ExtendedModel.Byte[][] _2Y_i = block((j, k) -> mul2(_1Y_i[j][k]));
      ExtendedModel.Byte[][] _3Y_i = block((j, k) -> em.xorVar(_1Y_i[j][k], _2Y_i[j][k]));

      ExtendedModel.Byte[][] δZ_i = new ExtendedModel.Byte[4][4];
      for (int k = 0; k < 4; k++) {
         δZ_i[0][k] = em.xorVar(_2Y_i[0][k], _3Y_i[1][k], _1Y_i[2][k], _1Y_i[3][k]);  // 2 3 1 1
         δZ_i[1][k] = em.xorVar(_1Y_i[0][k], _2Y_i[1][k], _3Y_i[2][k], _1Y_i[3][k]);  // 1 2 3 1
         δZ_i[2][k] = em.xorVar(_1Y_i[0][k], _1Y_i[1][k], _2Y_i[2][k], _3Y_i[3][k]);  // 1 1 2 3
         δZ_i[3][k] = em.xorVar(_3Y_i[0][k], _1Y_i[1][k], _1Y_i[2][k], _2Y_i[3][k]);  // 3 1 1 2
      }

      ExtendedModel.Byte[][] _09Z_i = block((j, k) -> mul9(δZ_i[j][k]));
      ExtendedModel.Byte[][] _11Z_i = block((j, k) -> mul11(δZ_i[j][k]));
      ExtendedModel.Byte[][] _13Z_i = block((j, k) -> mut13(δZ_i[j][k]));
      ExtendedModel.Byte[][] _14Z_i = block((j, k) -> mul14(δZ_i[j][k]));
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            // _rY_i = _1Y_i but using the reverse matrix (_rY_i is computed from Z_i)
            ExtendedModel.Byte[] _rY_i = new ExtendedModel.Byte[]{
                  em.xorVar(_14Z_i[0][k], _11Z_i[1][k], _13Z_i[2][k], _09Z_i[3][k]),    // 14 11 13  9
                  em.xorVar(_09Z_i[0][k], _14Z_i[1][k], _11Z_i[2][k], _13Z_i[3][k]),    //  9 14 11 13
                  em.xorVar(_13Z_i[0][k], _09Z_i[1][k], _14Z_i[2][k], _11Z_i[3][k]),    // 13  9 14 11
                  em.xorVar(_11Z_i[0][k], _13Z_i[1][k], _09Z_i[2][k], _14Z_i[3][k])     // 11 13  9 14
            };
            em.equals(_1Y_i[0][k], _rY_i[0]);
            em.equals(_1Y_i[1][k], _rY_i[1]);
            em.equals(_1Y_i[2][k], _rY_i[2]);
            em.equals(_1Y_i[3][k], _rY_i[3]);
         }
      }

      return δZ_i;
   }

   // KeySchedule utils
   private ExtendedModel.Byte[] elementsWiseXor(ExtendedModel.Byte[] vecA, ExtendedModel.Byte[] vecB) {
      return column((j) -> em.xorVar(vecA[j], vecB[j]));
   }

   private ExtendedModel.Byte[] getColumn(ExtendedModel.Byte[][] M, int k) {
      return column((j) -> M[j][k]);
   }

   private void setColumn(ExtendedModel.Byte[][] M, int k, ExtendedModel.Byte[] column) {
      for (int j = 0; j < M.length; j++) {
         M[j][k] = column[j];
      }
   }

   // Main utils
   private ExtendedModel.Byte subBytes(String name, ExtendedModel.Byte δ) {
      ExtendedModel.Byte SBδ = em.byteVar(name, 255, "SB(" + δ.name + ")");
      em.table(
            arrayOf(
                  δ.realization,
                  SBδ.realization,
                  newProbability()
            ),
            SBox.aes,
            STRATEGY
      );
      em.equals(δ.abstraction, SBδ.abstraction);
      _sBoxes.add(δ.abstraction);
      return SBδ;
   }

   private ExtendedModel.Byte mul2(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx2 = em.byteVar("2 * " + δ.name);
      em.table(arrayOf(δ.realization, δx2.realization), GaloisFieldMultiplication.mul2, STRATEGY);
      em.equals(δ.abstraction, δx2.abstraction);
      return δx2;
   }

   private ExtendedModel.Byte mul9(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx9 = em.byteVar("9 * " + δ.name);
      em.table(arrayOf(δ.realization, δx9.realization), GaloisFieldMultiplication.mul9, STRATEGY);
      em.equals(δ.abstraction, δx9.abstraction);
      return δx9;
   }

   private ExtendedModel.Byte mul11(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx11 = em.byteVar("11 * " + δ.name);
      em.table(arrayOf(δ.realization, δx11.realization), GaloisFieldMultiplication.mul11, STRATEGY);
      em.equals(δ.abstraction, δx11.abstraction);
      return δx11;
   }

   private ExtendedModel.Byte mut13(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx13 = em.byteVar("13 * " + δ.name);
      em.table(arrayOf(δ.realization, δx13.realization), GaloisFieldMultiplication.mul13, STRATEGY);
      em.equals(δ.abstraction, δx13.abstraction);
      return δx13;
   }

   private ExtendedModel.Byte mul14(ExtendedModel.Byte δ) {
      ExtendedModel.Byte δx14 = em.byteVar("14 * " + δ.name);
      em.table(arrayOf(δ.realization, δx14.realization), GaloisFieldMultiplication.mul14, STRATEGY);
      em.equals(δ.abstraction, δx14.abstraction);
      return δx14;
   }

   private IntVar newProbability() {
      IntVar p = em.intVar("probability[" + _probabilities.size() + "]", PROBABILITIES);
      _probabilities.add(p);
      return p;
   }

   private interface MatrixInitializer<T> {
      T get(int row, int col);
   }

   private interface ColumnInitializer<T> {
      T get(int row);
   }

   private ExtendedModel.Byte[] column(ColumnInitializer<ExtendedModel.Byte> initializer) {
      ExtendedModel.Byte[] matrix = new ExtendedModel.Byte[4];
      for (int j = 0; j < 4; j++) {
         matrix[j] = initializer.get(j);
      }
      return matrix;
   }

   private ExtendedModel.Byte[][] block(MatrixInitializer<ExtendedModel.Byte> initializer) {
      ExtendedModel.Byte[][] matrix = new ExtendedModel.Byte[4][4];
      for (int j = 0; j < 4; j++) {
         for (int k = 0; k < 4; k++) {
            matrix[j][k] = initializer.get(j, k);
         }
      }
      return matrix;
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
